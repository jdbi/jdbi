/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
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
package org.skife.jdbi.v2;

import com.google.common.base.Objects;
import com.google.common.collect.*;
import com.google.common.net.HostAndPort;
import org.junit.Test;
import org.skife.jdbi.v2.logging.PrintStreamLog;
import org.skife.jdbi.v2.sqlobject.SomethingMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.IntegerMapper;
import org.skife.jdbi.v2.util.StringMapper;
import org.skife.jdbi.v2.util.TimestampMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestMapQueries extends DBITestCase {

    private BasicHandle h;

    @Override
    public void doSetUp() throws Exception {
        h = openHandle();
        h.setSQLLog(new PrintStreamLog());
    }

    @Override
    public void doTearDown() throws Exception {
        if (h != null) h.close();
    }


    @Test
    public void testPrimitiveMappers() {
        insertDefaultData();

        Map<Integer, String> map = h.createMapQuery("select id, name from something")
                .mapKey(new IntegerMapper("id"))
                .mapValue(new StringMapper("name"))
                .get();
        Map<Integer, String> expected = ImmutableMap.of(1, "eric", 2, "brian");
        assertThat(map, equalTo(expected));
    }

    private void insertDefaultData() {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");
    }

    private void insertExtendedData() {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");
        h.insert("insert into something (id, name) values (3, 'david')");
        h.insert("insert into something (id, name) values (4, 'michael')");
        h.insert("insert into something (id, name) values (5, 'scott')");
        h.insert("insert into something (id, name) values (6, 'fred')");
    }

    @Test
    public void testPrimitiveKeyComplexValue() {
        insertDefaultData();

        Map<Integer, Something> map = h.createMapQuery("select id, name from something")
                .mapKey(new IntegerMapper("id"))
                .mapValue(new SomethingMapper())
                .get();

        Map<Integer, Something> expected = ImmutableMap.of(1, new Something(1, "eric"), 2, new Something(2, "brian"));
        assertThat(map, equalTo(expected));
    }

    @Test
    public void testComplexKeyPrimitiveValue() {
        h.createStatement("create table user_updates(id integer, name varchar(50), update_date timestamp)").execute();
        h.insert("insert into user_updates (id, name, update_date) values (1, 'eric', '2014-05-01 12:00:00')");
        h.insert("insert into user_updates (id, name, update_date) values (2, 'brian', '2014-05-06 13:00:00')");

        try {
            Map<Something, Timestamp> map = h.createMapQuery("select id, name, update_date from user_updates")
                    .mapKey(new SomethingMapper())
                    .mapValue(new TimestampMapper("update_date"))
                    .get();
            Map<Something, Timestamp> expected =
                    ImmutableMap.of(new Something(1, "eric"), timestamp("2014-05-01 12:00:00"),
                            new Something(2, "brian"), timestamp("2014-05-06 13:00:00"));
            assertThat(map, equalTo(expected));
        } finally {
            h.createStatement("drop table user_updates").execute();
        }
    }

    private static Timestamp timestamp(String textDate) {
        try {
            return new Timestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(textDate).getTime());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }


    @Test
    public void testComplexKeyComplexValue() {
        h.createStatement("create table user_servers(id integer, name varchar(50), ip varchar(15), port integer)").execute();
        h.insert("insert into user_servers (id, name, ip, port) values (1, 'eric', '192.168.52.15', 8080)");
        h.insert("insert into user_servers (id, name, ip, port) values (2, 'brian', '192.168.52.88', 9091)");

        try {
            Map<Something, HostAndPort> map = h.createMapQuery("select id, name, ip, port from user_servers")
                    .mapKey(new SomethingMapper())
                    .mapValue(new ResultSetMapper<HostAndPort>() {
                        @Override
                        public HostAndPort map(int index, ResultSet r, StatementContext ctx) throws SQLException {
                            return HostAndPort.fromParts(r.getString("ip"), r.getInt("port"));
                        }
                    }).get();
            Map<Something, HostAndPort> expected = ImmutableMap.of(
                    new Something(1, "eric"), HostAndPort.fromString("192.168.52.15:8080"),
                    new Something(2, "brian"), HostAndPort.fromString("192.168.52.88:9091"));
            assertThat(map, equalTo(expected));
        } finally {
            h.createStatement("drop table user_servers").execute();
        }
    }

    @Test
    public void testBeanKey() {
        insertDefaultData();

        Map<Something, Integer> map = h.createMapQuery("select id, name from something")
                .mapKey(Something.class)
                .mapValue(IntegerMapper.FIRST)
                .get();

        Map<Something, Integer> expected = ImmutableMap.of(new Something(1, "eric"), 1, new Something(2, "brian"), 2);
        assertThat(map, equalTo(expected));
    }

    @Test
    public void testBeanValue() {
        insertDefaultData();

        Map<Integer, Something> map = h.createMapQuery("select id, name from something")
                .mapKey(new IntegerMapper("id"))
                .mapValue(Something.class)
                .get();

        Map<Integer, Something> expected = ImmutableMap.of(1, new Something(1, "eric"), 2, new Something(2, "brian"));
        assertThat(map, equalTo(expected));
    }

    @Test
    public void testRegisteredMappers() {
        h.createStatement("create table user_servers(id integer, name varchar(50), ip varchar(15), port integer)").execute();
        h.insert("insert into user_servers (id, name, ip, port) values (1, 'eric', '192.168.52.15', 8080)");
        h.insert("insert into user_servers (id, name, ip, port) values (2, 'brian', '192.168.52.88', 9091)");

        try {
            h.registerMapper(new SomethingMapper());
            h.registerMapper(new ResultSetMapper<HostAndPort>() {
                @Override
                public HostAndPort map(int index, ResultSet r, StatementContext ctx) throws SQLException {
                    return HostAndPort.fromParts(r.getString("ip"), r.getInt("port"));
                }
            });
            Map<Something, HostAndPort> map = h.createMapQuery("select id, name, ip, port from user_servers")
                    .mapKeyTo(Something.class)
                    .mapValueTo(HostAndPort.class)
                    .get();
            Map<Something, HostAndPort> expected = ImmutableMap.of(
                    new Something(1, "eric"), HostAndPort.fromString("192.168.52.15:8080"),
                    new Something(2, "brian"), HostAndPort.fromString("192.168.52.88:9091"));
            assertThat(map, equalTo(expected));
        } finally {
            h.createStatement("drop table user_servers").execute();
        }
    }

    @Test
    public void testJoinedTables() {
        h.createScript("user-cities").execute();

        try {

            h.registerMapper(new SomethingMapper());
            h.registerMapper(new ResultSetMapper<City>() {
                @Override
                public City map(int index, ResultSet r, StatementContext ctx) throws SQLException {
                    return new City(r.getInt("city_id"), r.getString("city_name"), r.getString("state"));
                }
            });
            Map<Something, City> map = h.createMapQuery("select u.id, u.name, u.city_id, c.name city_name, c.state " +
                    "from users u " +
                    "inner join cities c on u.city_id = c.id")
                    .mapKeyTo(Something.class)
                    .mapValueTo(City.class)
                    .get();
            Map<Something, City> expected = ImmutableMap.<Something, City>builder()
                    .put(new Something(1, "eric"), new City(1, "Los Angeles", "CA"))
                    .put(new Something(2, "brian"), new City(1, "Los Angeles", "CA"))
                    .put(new Something(3, "david"), new City(2, "St. Louis", "MO"))
                    .put(new Something(4, "michael"), new City(3, "Boston", "MA"))
                    .put(new Something(5, "scott"), new City(3, "Boston", "MA"))
                    .put(new Something(6, "fred"), new City(1, "Los Angeles", "CA"))
                    .build();
            assertThat(map, equalTo(expected));
        } finally {
            h.createStatement("drop table users").execute();
            h.createStatement("drop table cities").execute();
        }
    }

    @Test
    public void testFolder() {
        h.createScript("user-cities").execute();

        try {
            h.registerMapper(new SomethingMapper());
            h.registerMapper(new ResultSetMapper<City>() {
                @Override
                public City map(int index, ResultSet r, StatementContext ctx) throws SQLException {
                    return new City(r.getInt("city_id"), r.getString("city_name"), r.getString("state"));
                }
            });

            Multimap<City, Something> multimap = h.createMapQuery("select u.id, u.name, u.city_id, c.name city_name, c.state " +
                    "from users u " +
                    "inner join cities c on u.city_id = c.id")
                    .mapKeyTo(Something.class)
                    .mapValueTo(City.class)
                    .fold(ArrayListMultimap.<City, Something>create(), new MapFolder<ArrayListMultimap<City, Something>, Something, City>() {
                        @Override
                        public void fold(ArrayListMultimap<City, Something> accumulator, Something key, City value) {
                            accumulator.put(value, key);
                        }
                    });

            Multimap<City, Something> expected = ImmutableMultimap.<City, Something>builder()
                    .putAll(new City(1, "Los Angeles", "CA"), new Something(1, "eric"), new Something(2, "brian"), new Something(6, "fred"))
                    .putAll(new City(2, "St. Louis", "MO"), new Something(3, "david"))
                    .putAll(new City(3, "Boston", "MA"), new Something(4, "michael"), new Something(5, "scott"))
                    .build();
            assertThat(multimap, equalTo(expected));
        } finally {
            h.createStatement("drop table users").execute();
            h.createStatement("drop table cities").execute();
        }
    }

    @Test
    public void testIterator() {
        insertExtendedData();

        ResultIterator<Map.Entry<Integer, String>> iterator = h.createMapQuery("select id, name from something")
                .mapKey(new IntegerMapper(1))
                .mapValue(new StringMapper(2))
                .iterator();
        try {
            int count = 0;
            List<Integer> shortNameIds = new ArrayList<Integer>(3);
            while (iterator.hasNext()) {
                Map.Entry<Integer, String> entry = iterator.next();
                if (entry.getValue().length() == 5) {
                    shortNameIds.add(entry.getKey());
                    if (++count >= 3) {
                        break;
                    }
                }
            }
            assertTrue(shortNameIds.size() == 3);
        } finally {
            iterator.close();
        }
    }

    @Test
    public void testMaxRows() {
        insertExtendedData();

        Map<Integer, String> map = h.createMapQuery("select id, name from something")
                .mapKey(new IntegerMapper(1))
                .mapValue(new StringMapper(2))
                .get(3);
        assertThat(map.size(), equalTo(3));
    }

    @Test
    public void testAccumulator() {
        insertExtendedData();

        Map<Integer, String> map = h.createMapQuery("select id, name from something")
                .mapKey(new IntegerMapper(1))
                .mapValue(new StringMapper(2))
                .accumulator(new TreeMap<Integer, String>(Ordering.natural().reverse()))
                .get();
        assertTrue(map.getClass().equals(TreeMap.class));
        assertTrue(map.keySet().equals(ImmutableSet.of(6, 5, 4, 3, 2, 1)));
    }


    private static class City {
        int id;
        String name;
        String state;

        City(int id, String name, String state) {
            this.id = id;
            this.name = name;
            this.state = state;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            City that = (City) o;

            return Objects.equal(id, that.id) && Objects.equal(name, that.name) &&
                    Objects.equal(state, that.state);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id, name, state);
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("id", id)
                    .add("name", name)
                    .add("state", state)
                    .toString();
        }
    }


}
