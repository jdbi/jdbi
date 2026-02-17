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
package org.jdbi.sqlobject;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.core.statement.UnableToCreateStatementException;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.customizer.BindMap;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.sqlobject.statement.UseRowMapper;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestBindMap {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;

    private Dao dao;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();

        dao = handle.attach(Dao.class);
    }

    @Test
    public void testBindMap() {
        handle.execute("insert into something (id, name) values (1, 'Alice')");

        dao.update(1, singletonMap("name", "Alicia"));

        assertThat(dao.get(1).getName()).isEqualTo("Alicia");
    }

    @Test
    public void testBindMapPrefixed() {
        handle.execute("insert into something (id, name) values (1, 'Alice')");

        dao.updatePrefix(1, singletonMap("name", "Alicia"));

        assertThat(dao.get(1).getName()).isEqualTo("Alicia");
    }

    @Test
    public void testBindMapKeyInKeysAndMap() {
        handle.execute("insert into something (id, name) values (2, 'Bob')");

        dao.updateNameKey(2, singletonMap("name", "Rob"));

        assertThat(dao.get(2).getName()).isEqualTo("Rob");
    }

    @Test
    public void testBindMapKeyInKeysNotInMap() {
        handle.execute("insert into something(id, name) values (2, 'Bob')");

        dao.updateNameKey(2, emptyMap());

        assertThat(dao.get(2).getName()).isNull();
    }

    @Test
    public void testBindMapKeyInMapNotInKeys() {
        handle.execute("insert into something(id, name) values (3, 'Carol')");

        dao.updateNameKey(3, ImmutableMap.of("name", "Cheryl", "integerValue", 3));

        assertThat(dao.get(3).getName()).isEqualTo("Cheryl");
        assertThat(dao.get(3).getIntegerValue()).isNull();
    }

    @Test
    public void testBindMapKeyNotInMapOrKeys() {
        handle.execute("insert into something(id, name) values (3, 'Carol')");
        Map<Object, Object> m = emptyMap();
        assertThatThrownBy(() -> dao.update(3, m)).isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testBindMapConvertKeysStringKeys() {
        handle.execute("insert into something(id, name) values (4, 'Dave')");

        dao.updateConvertKeys(4, singletonMap("name", "David"));

        assertThat(dao.get(4).getName()).isEqualTo("David");
    }

    @Test
    public void testBindMapConvertKeysNonStringKeys() {
        handle.execute("insert into something(id, name) values (4, 'Dave')");

        dao.updateConvertKeys(4, singletonMap(new MapKey("name"), "David"));

        assertThat(dao.get(4).getName()).isEqualTo("David");
    }

    @Test
    public void testBindMapNonStringKeys() {
        handle.execute("insert into something(id, name) values (5, 'Edward')");
        Map<Object, Object> m = singletonMap(new MapKey("name"), "Jacob");

        assertThatThrownBy(() -> dao.update(5, m)).isInstanceOf(IllegalArgumentException.class);
    }

    public static class MapKey {
        private final String value;

        MapKey(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public interface Dao {
        @SqlUpdate("update something set name=:name where id=:id")
        void update(@Bind int id, @BindMap Map<Object, Object> map);

        @SqlUpdate("update something set name=:map.name where id=:id")
        void updatePrefix(@Bind int id, @BindMap("map") Map<String, Object> map);

        @SqlUpdate("update something set name=:name where id=:id")
        void updateNameKey(@Bind int id, @BindMap(keys = "name") Map<String, Object> map);

        @SqlUpdate("update something set name=:name where id=:id")
        void updateConvertKeys(@Bind int id, @BindMap(convertKeys = true) Map<Object, Object> map);

        @SqlQuery("select * from something where id=:id")
        @UseRowMapper(SomethingMapper.class)
        Something get(long id);
    }
}
