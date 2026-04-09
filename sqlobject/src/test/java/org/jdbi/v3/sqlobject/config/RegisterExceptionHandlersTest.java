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

import java.sql.SQLException;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.statement.SqlExceptionHandler;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterExceptionHandlersTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withPlugin(new SqlObjectPlugin());

    private Handle h;

    @BeforeEach
    public void startUp() {
        this.h = h2Extension.openHandle();
        h.execute("create table exception_test(id int primary key)");
    }

    @AfterEach
    public void tearDown() {
        h.close();
    }

    @Test
    void handleExceptionDoubleRegister() {
        ExceptionMethodDao dao = h.attach(ExceptionMethodDao.class);
        dao.insert1();
        assertThatThrownBy(dao::insert1)
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessage("Wahoo");
    }

    @Test
    void handleExceptionSingleRegister() {
        ExceptionMethodDao dao = h.attach(ExceptionMethodDao.class);
        dao.insert1();
        assertThatThrownBy(dao::insert2)
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessage("Wahoo");
    }

    @Test
    void handleExceptionClassRegister() {
        ExceptionClassDao dao = h.attach(ExceptionClassDao.class);
        dao.insert1();
        assertThatThrownBy(dao::insert1)
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessage("Wahoo");
    }

    public interface ExceptionMethodDao {
        @RegisterSqlExceptionHandler(HandlerOne.class)
        @RegisterSqlExceptionHandler(HandlerTwo.class)
        @SqlUpdate("insert into exception_test (id) values(1)")
        void insert1();

        @RegisterSqlExceptionHandler(HandlerTwo.class)
        @SqlUpdate("insert into exception_test (id) values(1)")
        void insert2();
    }


    @RegisterSqlExceptionHandler(HandlerTwo.class)
    public interface ExceptionClassDao {
        @SqlUpdate("insert into exception_test (id) values(1)")
        void insert1();
    }

    public static class HandlerOne implements SqlExceptionHandler {
        @Override
        public void handle(SQLException ex, StatementContext ctx) {} // chaining test
    }

    public static class HandlerTwo implements SqlExceptionHandler {
        @Override
        public void handle(SQLException ex, StatementContext ctx) {
            if ("23505".equals(ex.getSQLState())) {
                throw new RuntimeException("Wahoo");
            }
        }
    }
}
