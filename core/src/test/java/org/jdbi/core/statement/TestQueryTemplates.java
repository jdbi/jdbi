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
import org.jdbi.core.junit5.H2DatabaseExtension;
import org.jdbi.core.result.ResultIterable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.stream.Collectors.toMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class TestQueryTemplates {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    @Test
    public void testCreateQueryObject() {
        final Handle h = h2Extension.getSharedHandle();

        h.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        h.createUpdate("insert into something (id, name) values (2, 'brian')").execute();

        final var queryTemplate = h.getJdbi().buildQueryTemplate("select * from something order by id")
                .mapToMap();

        final List<Map<String, Object>> results = queryTemplate.with(h).execute().list();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).get("name")).isEqualTo("eric");
    }

    @Test
    public void testMappedQueryObject() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        final var queryTemplate = h.getJdbi().buildQueryTemplate("select * from something order by id")
                .mapToBean(Something.class);

        final ResultIterable<Something> query = queryTemplate.with(h).execute();

        final List<Something> r = query.list();
        assertThat(r).startsWith(new Something(1, "eric"));
    }

    @Test
    public void testMappedQueryObjectWithNulls() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name, integerValue) values (1, 'eric', null)");

        final var queryTemplate = h.getJdbi().buildQueryTemplate("select * from something order by id")
                .mapToBean(Something.class);

        final ResultIterable<Something> query = queryTemplate.with(h).execute();

        final List<Something> r = query.list();
        final Something eric = r.get(0);
        assertThat(eric).isEqualTo(new Something(1, "eric"));
        assertThat(eric.getIntegerValue()).isNull();
    }

    @Test
    public void testMappedQueryObjectWithNullForPrimitiveIntField() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name, intValue) values (1, 'eric', null)");

        final var queryTemplate = h.getJdbi().buildQueryTemplate("select * from something order by id")
                .mapToBean(Something.class);

        final ResultIterable<Something> query = queryTemplate.with(h).execute();

        final List<Something> r = query.list();
        final Something eric = r.get(0);
        assertThat(eric).isEqualTo(new Something(1, "eric"));
        assertThat(eric.getIntValue()).isZero();
    }

    @Test
    public void testMapper() {
        final Handle h = h2Extension.getSharedHandle();

        h.execute("insert into something (id, name) values (1, 'eric')");
        h.execute("insert into something (id, name) values (2, 'brian')");

        final var queryTemplate = h.getJdbi().buildQueryTemplate("select name from something order by id")
                .map((r, ctx) -> r.getString(1));

        final ResultIterable<String> query = queryTemplate.with(h).execute();

        final List<String> r = query.list();
        assertThat(r).startsWith("eric");
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

}
