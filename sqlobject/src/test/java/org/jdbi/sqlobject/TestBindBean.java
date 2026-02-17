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

import java.sql.Types;

import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.core.ValueType;
import org.jdbi.core.argument.AbstractArgumentFactory;
import org.jdbi.core.argument.Argument;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.mapper.ValueTypeMapper;
import org.jdbi.sqlobject.config.RegisterColumnMapper;
import org.jdbi.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestBindBean {

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
    public void testBindBean() {
        handle.execute("insert into something (id, name) values (1, 'Alice')");
        assertThat(dao.getName(1)).isEqualTo("Alice");

        dao.update(new Something(1, "Alicia"));
        assertThat(dao.getName(1)).isEqualTo("Alicia");
    }

    @Test
    public void testBindBeanPrefix() {
        handle.execute("insert into something (id, name) values (2, 'Bob')");
        assertThat(dao.getName(2)).isEqualTo("Bob");

        dao.updatePrefix(new Something(2, "Rob"));
        assertThat(dao.getName(2)).isEqualTo("Rob");
    }

    public interface Dao {
        @SqlUpdate("update something set name=:name where id=:id")
        void update(@BindBean Something thing);

        @SqlUpdate("update something set name=:thing.name where id=:thing.id")
        void updatePrefix(@BindBean("thing") Something thing);

        @SqlQuery("select name from something where id = :id")
        String getName(long id);
    }

    @Test
    public void testNoArgumentFactoryRegisteredForProperty() {
        handle.execute("create table beans (id integer, value_type varchar)");

        assertThatThrownBy(() -> handle.attach(BeanDao.class).insert(new Bean(1, ValueType.valueOf("foo"))))
                .hasMessageContaining("No argument factory registered");
    }

    @Test
    public void testArgumentFactoryRegisteredForProperty() {
        handle.execute("create table beans (id integer, value_type varchar, fromField varchar, fromGetter varchar)");
        handle.registerArgument(new ValueTypeArgumentFactory());

        BeanDao beanDao = handle.attach(BeanDao.class);

        beanDao.insert(new Bean(1, ValueType.valueOf("foo")));
        assertThat(beanDao.getById(1)).extracting(Bean::getId, Bean::getValueType)
                .containsExactly(1, ValueType.valueOf("foo"));
    }

    public static class Bean {
        private int id;
        private ValueType valueType;

        public Bean(int id, ValueType valueType) {
            this.id = id;
            this.valueType = valueType;
        }

        public int getId() {
            return id;
        }

        public ValueType getValueType() {
            return valueType;
        }

        public int getWithParameterIsIgnored(int param) {
            return param;
        }
    }

    public static class ValueTypeArgumentFactory extends AbstractArgumentFactory<ValueType> {
        public ValueTypeArgumentFactory() {
            super(Types.VARCHAR);
        }

        @Override
        protected Argument build(ValueType value, ConfigRegistry config) {
            return (pos, stmt, ctx) -> stmt.setString(pos, value.getValue());
        }
    }

    public interface BeanDao {
        @SqlUpdate("insert into beans (id, value_type) values (:id, :valueType)")
        void insert(@BindBean Bean bean);

        @SqlQuery("select * from beans where id = :id")
        @RegisterConstructorMapper(Bean.class)
        @RegisterColumnMapper(ValueTypeMapper.class)
        Bean getById(int id);
    }
}
