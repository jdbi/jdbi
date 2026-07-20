/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.core.statement;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.google.common.collect.Maps;
import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.Something;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.config.ConfigView;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.jdbi.core.mapper.NoSuchMapperException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

public class TestStatementTemplates {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    @Test
    public void testCreateQueryObject() {
        final Handle h = h2Extension.getSharedHandle();

        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        final var queryTemplate = h.getJdbi().buildStatementTemplate("select * from something order by id");

        final List<Map<String, Object>> results = queryTemplate.with(h).mapToMap().list();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).get("name")).isEqualTo("eric");
    }

    @Test
    public void testMappedQueryObject() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        final var queryTemplate = h.getJdbi().buildStatementTemplate("select * from something order by id");

        final List<Something> r = queryTemplate.with(h).mapToBean(Something.class).list();
        assertThat(r).startsWith(new Something(1, "eric"));
    }

    @Test
    public void testMappedQueryObjectWithNulls() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name, integerValue) values (1, 'eric', null)");

        final var queryTemplate = h.getJdbi().buildStatementTemplate("select * from something order by id");

        final List<Something> r = queryTemplate.with(h).mapToBean(Something.class).list();
        final Something eric = r.get(0);
        assertThat(eric).isEqualTo(new Something(1, "eric"));
        assertThat(eric.getIntegerValue()).isNull();
    }

    @Test
    public void testMappedQueryObjectWithNullForPrimitiveIntField() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name, intValue) values (1, 'eric', null)");

        final var queryTemplate = h.getJdbi().buildStatementTemplate("select * from something order by id");

        final List<Something> r = queryTemplate.with(h).mapToBean(Something.class).list();
        final Something eric = r.get(0);
        assertThat(eric).isEqualTo(new Something(1, "eric"));
        assertThat(eric.getIntValue()).isZero();
    }

    @Test
    public void testMapper() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        final var queryTemplate = h.getJdbi().buildStatementTemplate("select name from something order by id");

        final List<String> r = queryTemplate.with(h).map((rs, ctx) -> rs.getString(1)).list();
        assertThat(r).startsWith("eric");
    }

    @Test
    public void testReuseAcrossExecutions() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        // One template, bound and executed twice with different parameters.
        final var queryTemplate = h.getJdbi().buildStatementTemplate("select name from something where id = :id");

        assertThat(queryTemplate.with(h).bind("id", 1).mapTo(String.class).one()).isEqualTo("eric");
        assertThat(queryTemplate.with(h).bind("id", 2).mapTo(String.class).one()).isEqualTo("brian");
    }

    @Test
    public void testPerExecutionMaxRows() {
        final Handle h = h2Extension.getSharedHandle();

        h.prepareBatch("insert into something (id, name) values (?, ?)")
            .add(1, "eric")
            .add(2, "brian")
            .add(3, "keith")
            .execute();

        final var queryTemplate = h.getJdbi().buildStatementTemplate("select name from something order by id");

        // The customizer is recorded on the binding, so it caps only this execution...
        assertThat(queryTemplate.with(h).setMaxRows(1).mapTo(String.class).list()).containsExactly("eric");
        // ...and does not leak into the shared template: the next execution sees every row.
        assertThat(queryTemplate.with(h).mapTo(String.class).list()).containsExactly("eric", "brian", "keith");
    }

    @Test
    public void testReduceRows() {
        final Handle h = h2Extension.getSharedHandle();

        h.prepareBatch("insert into something (id, name) values (?, ?)")
            .add(1, "Brian")
            .add(2, "Keith")
            .execute();

        final var queryTemplate = h.getJdbi().buildStatementTemplate("select id, name from something order by id");

        // Exercises the reduceRows path, which returns a Stream and cannot be expressed by a
        // build-time-baked ResultIterable scanner; it works because the binding is a ResultBearing.
        final List<String> names = queryTemplate.with(h)
            .<Integer, String>reduceRows((map, rowView) ->
                map.put(rowView.getColumn("id", Integer.class), rowView.getColumn("name", String.class)))
            .collect(toList());

        assertThat(names).containsExactly("Brian", "Keith");
    }

    @Test
    public void testMappedTemplateReuse() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        // The mapper is resolved once here; every execution below reuses it.
        final var byId = h.getJdbi()
            .buildStatementTemplate("select name from something where id = :id")
            .mapTo(String.class);

        assertThat(byId.with(h).bind("id", 1).results().one()).isEqualTo("eric");
        assertThat(byId.with(h).bind("id", 2).results().one()).isEqualTo("brian");
    }

    @Test
    public void testMappedTemplateList() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        final var names = h.getJdbi()
            .buildStatementTemplate("select name from something order by id")
            .mapTo(String.class);

        assertThat(names.with(h).results().list()).containsExactly("eric", "brian");
    }

    @Test
    public void testMappedTemplateGenericType() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");

        final var names = h.getJdbi()
            .buildStatementTemplate("select name from something order by id")
            .mapTo(new GenericType<String>() {});

        assertThat(names.with(h).results().one()).isEqualTo("eric");
    }

    @Test
    public void testMappedTemplatePerExecutionCustomizer() {
        final Handle h = h2Extension.getSharedHandle();

        h.prepareBatch("insert into something (id, name) values (?, ?)")
            .add(1, "eric")
            .add(2, "brian")
            .add(3, "keith")
            .execute();

        final var names = h.getJdbi()
            .buildStatementTemplate("select name from something order by id")
            .mapTo(String.class);

        // The customizer is recorded on this binding only, so it caps this execution...
        assertThat(names.with(h).setMaxRows(1).results().list()).containsExactly("eric");
        // ...and does not leak into the shared mapped template.
        assertThat(names.with(h).results().list()).containsExactly("eric", "brian", "keith");
    }

    @Test
    public void testMappedTemplateDefine() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        // A defined attribute supplied per execution re-renders the SQL for that execution.
        final var byColumn = h.getJdbi()
            .buildStatementTemplate("select <column> from something where id = 1")
            .mapTo(String.class);

        assertThat(byColumn.with(h).define("column", "name").results().one()).isEqualTo("eric");
    }

    @Test
    public void testMappedTemplateUnknownTypeFailsAtBuild() {
        final Handle h = h2Extension.getSharedHandle();

        final var queryTemplate = h.getJdbi().buildStatementTemplate("select name from something");

        // Resolution happens eagerly, so an unmapped type is reported when the mapped template is built,
        // not later at execution time.
        assertThatThrownBy(() -> queryTemplate.mapTo(Unmapped.class))
            .isInstanceOf(NoSuchMapperException.class);
    }

    private static final class Unmapped {}

    @Test
    public void testMappedTemplateMapRowMapper() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        // map(RowMapper) bakes a mapper the caller already holds, without consulting the registry --
        // and exercises the genuine row-mapper branch (not the column-mapper wrapper).
        final var somethings = h.getJdbi()
            .buildStatementTemplate("select id, name from something order by id")
            .map((rs, ctx) -> new Something(rs.getInt("id"), rs.getString("name")));

        assertThat(somethings.with(h).results().list())
            .containsExactly(new Something(1, "eric"), new Something(2, "brian"));
    }

    @Test
    public void testMappedTemplateMapColumnMapper() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");

        final var names = h.getJdbi()
            .buildStatementTemplate("select name from something order by id")
            .map((rs, col, ctx) -> rs.getString(col));

        assertThat(names.with(h).results().one()).isEqualTo("eric");
    }

    @Test
    public void testMappedTemplateRawType() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");

        final Type type = String.class;
        final var names = h.getJdbi()
            .buildStatementTemplate("select name from something order by id")
            .mapTo(type);

        assertThat(names.with(h).results().one()).isEqualTo("eric");
    }

    @Test
    public void testMappedTemplateStream() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        final var names = h.getJdbi()
            .buildStatementTemplate("select name from something order by id")
            .mapTo(String.class);

        try (Stream<String> stream = names.with(h).results().stream()) {
            assertThat(stream.collect(toList())).containsExactly("eric", "brian");
        }
    }

    @Test
    public void testMappedTemplateMatchesPlainPath() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        // The mapped path must return exactly what the plain per-execution mapTo(X) path returns.
        final var template = h.getJdbi().buildStatementTemplate("select name from something order by id");
        final var mapped = template.mapTo(String.class);

        assertThat(mapped.with(h).results().list())
            .isEqualTo(template.with(h).mapTo(String.class).list());
    }

    @Test
    public void testMappedTemplateReuseAcrossHandles() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        // One template, executed against two independent handles.
        final var byId = h.getJdbi()
            .buildStatementTemplate("select name from something where id = :id")
            .mapTo(String.class);

        try (Handle other = h.getJdbi().open()) {
            assertThat(byId.with(h).bind("id", 1).results().one()).isEqualTo("eric");
            assertThat(byId.with(other).bind("id", 2).results().one()).isEqualTo("brian");
        }
    }

    @Test
    public void testTemplateConfigIsHandleIndependent() {
        // A statement template captures its Jdbi-level configuration at build time. A handle carries no
        // configuration of its own -- only its connection and transaction state -- so with(handle) uses the
        // template's configuration regardless of which handle, or which Jdbi's handle, it runs on. This is what
        // makes a template built from one Jdbi correct to execute against any handle from a compatible connection.
        final Handle shared = h2Extension.getSharedHandle();
        shared.execute("insert into something (id, name) values (1, 'eric')");

        // A Jdbi that knows how to map the name column to a Widget, derived so it shares the connection source.
        final Jdbi configured = shared.getJdbi().toBuilder()
            .registerColumnMapper(Widget.class, (rs, col, ctx) -> new Widget(rs.getString(col)))
            .build();
        final var template = configured.buildStatementTemplate("select name from something where id = 1")
            .mapTo(Widget.class);

        try (Handle bare = shared.getJdbi().open()) {
            // The bare handle's Jdbi has no Widget mapper, so it cannot map the type on its own...
            assertThatThrownBy(() -> bare.createQuery("select name from something where id = 1").mapTo(Widget.class).one())
                .isInstanceOf(NoSuchMapperException.class);
            // ...but the template supplies the mapper from its own (Jdbi-level) configuration.
            assertThat(template.with(bare).results().one()).isEqualTo(new Widget("eric"));
        }
    }

    static final class Widget {
        final String name;

        Widget(final String name) {
            this.name = name;
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof Widget && ((Widget) o).name.equals(name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return "Widget[" + name + ']';
        }
    }

    @Test
    public void testFold() {
        final Handle h = h2Extension.getSharedHandle();

        h.prepareBatch("insert into something (id, name) values (?, ?)")
            .add(1, "Brian")
            .add(2, "Keith")
            .execute();

        final Map<String, Integer> rs = h.createQuery("select id, name from something")
            .<Entry<String, Integer>>map((r, ctx) -> Maps.immutableEntry(r.getString("name"), r.getInt("id")))
            .collect(toMap(Entry::getKey, Entry::getValue));

        assertThat(rs).containsOnly(entry("Brian", 1), entry("Keith", 2));
    }

    @Test
    public void testTemplateDrivesUpdateAndQuery() {
        final Handle h = h2Extension.getSharedHandle();

        // One reusable template, run as an update: which terminal you call picks how it executes.
        final var insert = h.getJdbi()
            .buildStatementTemplate("insert into something (id, name) values (:id, :name)");
        assertThat(insert.with(h).bind("id", 1).bind("name", "eric").execute()).isEqualTo(1);
        assertThat(insert.with(h).bind("id", 2).bind("name", "brian").execute()).isEqualTo(1);

        // A query template, run for results, sees both rows.
        final var names = h.getJdbi()
            .buildStatementTemplate("select name from something order by id")
            .mapTo(String.class);
        assertThat(names.with(h).results().list()).containsExactly("eric", "brian");
    }

    @Test
    public void testPreparedBatchViaTemplate() {
        final Handle h = h2Extension.getSharedHandle();

        final var template = h.getJdbi()
            .buildStatementTemplate("insert into something (id, name) values (:id, :name)");

        final int[] counts = template.prepareBatch(h)
            .add(Map.of("id", 1, "name", "eric"))
            .add(Map.of("id", 2, "name", "brian"))
            .execute();
        assertThat(counts).containsExactly(1, 1);

        // Reused for a second batch execution against the same template.
        template.prepareBatch(h).add(Map.of("id", 3, "name", "keith")).execute();

        assertThat(h.createQuery("select count(*) from something").mapTo(int.class).one()).isEqualTo(3);
    }

    // Regression guard: a per-execution define() is statement state (a defines overlay), not configuration.
    // Before the overlay redesign, define() forked the statement's copy-on-write config child, which cleared
    // the registry's memoized resolver views and forced every mapper/argument/collector to re-resolve cold.
    // It must now leave the config (and its warm resolvers) untouched while still affecting rendering.
    @Test
    public void defineKeepsResolverViewsWarm() {
        final Handle h = h2Extension.getSharedHandle();

        final Query query = h.createQuery("select <col> from something");
        final ConfigRegistry config = query.getContext().getConfig();

        // Warm a memoized resolver view on the statement's config.
        final Object warmView = config.readAs(WarmthProbe.class, WarmthProbe::new);

        query.define("col", "name");

        // The define did not fork the config, so the memoized view is the same warm instance, not rebuilt cold.
        assertThat(config.readAs(WarmthProbe.class, WarmthProbe::new)).isSameAs(warmView);
        // ...and the define still takes effect for rendering.
        assertThat(query.getContext().getAttribute("col")).isEqualTo("name");
    }

    // A distinct memoized-view type used only to observe whether define() invalidates the resolver cache.
    private static final class WarmthProbe {
        WarmthProbe(final ConfigView config) {}
    }

}
