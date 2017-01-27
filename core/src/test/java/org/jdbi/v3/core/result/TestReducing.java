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
package org.jdbi.v3.core.result;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestReducing
{
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Before
    public void setUp()
    {
        Handle h = dbRule.getSharedHandle();
        h.execute("CREATE TABLE something_location (id int, location varchar)");
        h.execute("INSERT INTO something (id, name) VALUES (1, 'tree')");
        h.execute("INSERT INTO something (id, name) VALUES (2, 'apple')");
        h.execute("INSERT INTO something_location (id, location) VALUES (1, 'outside')");
        h.execute("INSERT INTO something_location (id, location) VALUES (2, 'tree')");
        h.execute("INSERT INTO something_location (id, location) VALUES (2, 'pie')");
        h.registerRowMapper(new SomethingMapper());
    }

    @Test
    public void testLeftJoinRowView() throws Exception
    {
        Map<Integer, SomethingWithLocations> result = dbRule.getSharedHandle()
            .createQuery("SELECT something.id, name, location FROM something NATURAL JOIN something_location")
            .reduceRows(new HashMap<Integer, SomethingWithLocations>(), (map, rr) -> {
                map.computeIfAbsent(rr.getColumn("id", Integer.class),
                        id -> new SomethingWithLocations(rr.getRow(Something.class)))
                    .locations.add(rr.getColumn("location", String.class));
                return map;
            });

        assertThat(result).hasSize(2)
            .containsEntry(1, new SomethingWithLocations(new Something(1, "tree")).at("outside"))
            .containsEntry(2, new SomethingWithLocations(new Something(2, "apple")).at("tree").at("pie"));
    }

    @Test
    public void testLeftJoinResultSet() throws Exception
    {
        Map<Integer, SomethingWithLocations> result = dbRule.getSharedHandle()
            .createQuery("SELECT something.id, name, location FROM something NATURAL JOIN something_location")
            .reduceResultSet(new HashMap<Integer, SomethingWithLocations>(), (map, rs, ctx) -> {
                final String name = rs.getString("name");
                map.computeIfAbsent(rs.getInt("id"),
                        id -> new SomethingWithLocations(new Something(id, name)))
                    .at(rs.getString("location"));
                return map;
            });

        assertThat(result).hasSize(2)
            .containsEntry(1, new SomethingWithLocations(new Something(1, "tree")).at("outside"))
            .containsEntry(2, new SomethingWithLocations(new Something(2, "apple")).at("tree").at("pie"));
    }

    static class SomethingWithLocations
    {
        final Something something;
        final List<String> locations = new ArrayList<>();

        SomethingWithLocations(Something something)
        {
            this.something = something;
        }

        SomethingWithLocations at(String where)
        {
            locations.add(where);
            return this;
        }

        @Override
        public boolean equals(Object other)
        {
            if (!(other instanceof SomethingWithLocations))
            {
                return false;
            }
            SomethingWithLocations o = (SomethingWithLocations)other;
            return o.something.equals(something) && o.locations.equals(locations);
        }

        @Override
        public int hashCode()
        {
            return something.hashCode();
        }

        @Override
        public String toString()
        {
            return String.format("Something %s with locations %s", something, locations);
        }
    }
}
