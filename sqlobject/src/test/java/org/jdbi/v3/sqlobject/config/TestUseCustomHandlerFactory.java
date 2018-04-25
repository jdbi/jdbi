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
package org.jdbi.v3.sqlobject.config;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.Handler;
import org.jdbi.v3.sqlobject.HandlerFactory;
import org.jdbi.v3.sqlobject.Handlers;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUseCustomHandlerFactory {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception {
        Jdbi db = dbRule.getJdbi();

        HandlerFactory defaultHandlerFactory = new HandlerFactory() {

            @Override
            public Optional<Handler> buildHandler(Class<?> sqlObjectType, Method method) {
                return getImplementation(sqlObjectType, method).map(m ->
                        (target, args, h) -> m.invoke(null, Stream.concat(Stream.of(target), Stream.of(args)).toArray())
               );
            }

            private Optional<Method> getImplementation(Class<?> type, Method method) {
                return Stream.of(type.getClasses())
                        .filter(c -> c.getSimpleName().equals("DefaultImpls"))
                        .flatMap(c -> Stream.of(c.getMethods()).filter(m -> m.getName().equals(method.getName())))
                        .findAny();
            }
        };

        db.configure(Handlers.class, c -> c.register(defaultHandlerFactory));
        handle = db.open();
    }

    @Test
    public void shouldUseConfiguredDefaultHandler() throws Exception {
        SomethingDao h = handle.attach(SomethingDao.class);
        Something s = h.insertAndFind(new Something(1, "Joy"));
        assertThat(s.getName()).isEqualTo("Joy");
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface SomethingDao {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindBean Something s);

        @SqlQuery("select id, name from something where id = :id")
        Something findById(@Bind("id") int id);

        @Transaction
        Something insertAndFind(Something s);

        @SuppressWarnings("unused")
        class DefaultImpls {
            private DefaultImpls() {}

            public static Something insertAndFind(SomethingDao dao, Something s) {
                dao.insert(s);
                return dao.findById(s.getId());
            }
        }
    }

}
