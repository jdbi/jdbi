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
package org.jdbi.v3.sqlobject;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.extension.ExtensionMethod;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.junit.Rule;
import org.junit.Test;

public class TestDefaultMethods
{
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testDefaultMethod() throws Exception {
        Spiffy dao = dbRule.getJdbi().onDemand(Spiffy.class);
        Something test = dao.insertAndReturn(3, "test");
        assertThat(test).isEqualTo(new Something(3, "test"));
    }

    @Test
    public void testOverride() throws Exception {
        SpiffyOverride dao = dbRule.getJdbi().onDemand(SpiffyOverride.class);
        assertThat(dao.insertAndReturn(123, "fake")).isNull();
    }

    @Test
    public void testOverrideWithDefault() throws Exception {
        SpiffyOverrideWithDefault dao = dbRule.getJdbi().onDemand(SpiffyOverrideWithDefault.class);
        assertThat(dao.insertAndReturn(123, "fake").getId()).isEqualTo(-6);
    }

    private interface Spiffy
    {
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

    public interface SpiffyOverride extends Spiffy
    {
        @Override
        @UseRowMapper(SomethingMapper.class)
        @SqlQuery("select id, name from something where id = :id")
        Something insertAndReturn(@Bind int id, @Bind String name);
    }

    public interface SpiffyOverrideWithDefault extends SpiffyOverride
    {
        @Override
        default Something insertAndReturn(int id, String name) {
            return new Something(-6, "what");
        }
    }

    @Test
    public void testHandleHasExtensionMethodSet() throws Exception {
        dbRule.getJdbi().useExtension(StatementContextExtensionMethodDao.class, dao -> dao.check());
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
