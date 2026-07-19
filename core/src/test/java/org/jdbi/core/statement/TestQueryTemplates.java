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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;
import org.jdbi.core.Handle;
import org.jdbi.core.Something;
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

public class TestQueryTemplates {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    @Test
    public void testCreateQueryObject() {
        final Handle h = h2Extension.getSharedHandle();

        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        final var queryTemplate = h.getJdbi().buildQueryTemplate("select * from something order by id");

        final List<Map<String, Object>> results = queryTemplate.with(h).mapToMap().list();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).get("name")).isEqualTo("eric");
    }

    @Test
    public void testMappedQueryObject() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        final var queryTemplate = h.getJdbi().buildQueryTemplate("select * from something order by id");

        final List<Something> r = queryTemplate.with(h).mapToBean(Something.class).list();
        assertThat(r).startsWith(new Something(1, "eric"));
    }

    @Test
    public void testMappedQueryObjectWithNulls() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name, integerValue) values (1, 'eric', null)");

        final var queryTemplate = h.getJdbi().buildQueryTemplate("select * from something order by id");

        final List<Something> r = queryTemplate.with(h).mapToBean(Something.class).list();
        final Something eric = r.get(0);
        assertThat(eric).isEqualTo(new Something(1, "eric"));
        assertThat(eric.getIntegerValue()).isNull();
    }

    @Test
    public void testMappedQueryObjectWithNullForPrimitiveIntField() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name, intValue) values (1, 'eric', null)");

        final var queryTemplate = h.getJdbi().buildQueryTemplate("select * from something order by id");

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

        final var queryTemplate = h.getJdbi().buildQueryTemplate("select name from something order by id");

        final List<String> r = queryTemplate.with(h).map((rs, ctx) -> rs.getString(1)).list();
        assertThat(r).startsWith("eric");
    }

    @Test
    public void testReuseAcrossExecutions() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        // One template, bound and executed twice with different parameters.
        final var queryTemplate = h.getJdbi().buildQueryTemplate("select name from something where id = :id");

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

        final var queryTemplate = h.getJdbi().buildQueryTemplate("select name from something order by id");

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

        final var queryTemplate = h.getJdbi().buildQueryTemplate("select id, name from something order by id");

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
            .buildQueryTemplate("select name from something where id = :id")
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
            .buildQueryTemplate("select name from something order by id")
            .mapTo(String.class);

        assertThat(names.with(h).results().list()).containsExactly("eric", "brian");
    }

    @Test
    public void testMappedTemplateGenericType() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");

        final var names = h.getJdbi()
            .buildQueryTemplate("select name from something order by id")
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
            .buildQueryTemplate("select name from something order by id")
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
            .buildQueryTemplate("select <column> from something where id = 1")
            .mapTo(String.class);

        assertThat(byColumn.with(h).define("column", "name").results().one()).isEqualTo("eric");
    }

    @Test
    public void testMappedTemplateUnknownTypeFailsAtBuild() {
        final Handle h = h2Extension.getSharedHandle();

        final var queryTemplate = h.getJdbi().buildQueryTemplate("select name from something");

        // Resolution happens eagerly, so an unmapped type is reported when the mapped template is built,
        // not later at execution time.
        assertThatThrownBy(() -> queryTemplate.mapTo(Unmapped.class))
            .isInstanceOf(NoSuchMapperException.class);
    }

    private static final class Unmapped {}

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

}
