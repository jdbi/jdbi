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

import java.lang.reflect.Method;

import org.jdbi.core.Something;
import org.jdbi.core.extension.ExtensionMethod;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.sqlobject.statement.UseRowMapper;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDefaultMethods {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    @Test
    public void testDefaultMethod() {
        Spiffy dao = h2Extension.getJdbi().onDemand(Spiffy.class);
        Something test = dao.insertAndReturn(3, "test");
        assertThat(test).isEqualTo(new Something(3, "test"));
    }

    @Test
    public void testOverride() {
        SpiffyOverride dao = h2Extension.getJdbi().onDemand(SpiffyOverride.class);
        assertThat(dao.insertAndReturn(123, "fake")).isNull();
    }

    @Test
    public void testOverrideWithDefault() {
        SpiffyOverrideWithDefault dao = h2Extension.getJdbi().onDemand(SpiffyOverrideWithDefault.class);
        assertThat(dao.insertAndReturn(123, "fake").getId()).isEqualTo(-6);
    }

    private interface Spiffy {
        @SqlQuery("select id, name from something where id = :id")
        @UseRowMapper(SomethingMapper.class)
        Something byId(@Bind("id") int id);

        @SqlUpdate("insert into something (id, name) values (:it.id, :it.name)")
        void insert(@BindSomething("it") Something it);

        default Something insertAndReturn(int id, String name) {
            insert(new Something(id, name));
            return byId(id);
        }
    }

    public interface SpiffyOverride extends Spiffy {
        @Override
        @UseRowMapper(SomethingMapper.class)
        @SqlQuery("select id, name from something where id = :id")
        Something insertAndReturn(@Bind int id, @Bind String name);
    }

    public interface SpiffyOverrideWithDefault extends SpiffyOverride {
        @Override
        default Something insertAndReturn(int id, String name) {
            return new Something(-6, "what");
        }
    }

    @Test
    public void testHandleHasExtensionMethodSet() throws Exception {
        h2Extension.getJdbi().useExtension(StatementContextExtensionMethodDao.class, StatementContextExtensionMethodDao::check);
    }

    private interface StatementContextExtensionMethodDao extends SqlObject {
        default void check() throws Exception {
            Class<StatementContextExtensionMethodDao> extensionMethodDaoClass = StatementContextExtensionMethodDao.class;
            Method checkMethod = extensionMethodDaoClass.getMethod("check");

            ExtensionMethod extensionMethod = getHandle().getExtensionMethod();
            assertThat(extensionMethod.getType()).isEqualTo(extensionMethodDaoClass);
            assertThat(extensionMethod.getMethod()).isEqualTo(checkMethod);

            extensionMethod = getHandle().createQuery("select * from something").getContext().getExtensionMethod();
            assertThat(extensionMethod.getType()).isEqualTo(extensionMethodDaoClass);
            assertThat(extensionMethod.getMethod()).isEqualTo(checkMethod);
        }
    }
}
