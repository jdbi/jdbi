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
package org.jdbi.v3.core;

import java.sql.SQLException;

import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.statement.SqlExceptionHandler;
import org.jdbi.v3.core.statement.SqlStatements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HandleExceptionTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private Handle h;

    @BeforeEach
    public void startUp() {
        this.h = h2Extension.openHandle();
    }

    @AfterEach
    public void tearDown() {
        h.close();
    }

    @Test
    void handleException() {
        h.getConfig(SqlStatements.class)
                .addExceptionHandler(s -> null)
                .addExceptionHandler(new SqlExceptionHandler() {
                        @Override
                        public Throwable handle(SQLException ex) {
                            if ("23505".equals(ex.getSQLState())) {
                                return new RuntimeException("Wahoo");
                            }
                            return null;
                        }
                })
                .addExceptionHandler(s -> null); // simple chaining test
        h.execute("create table exception_test(id int primary key)");
        final String sql = "insert into exception_test (id) values(1)";
        h.execute(sql);
        assertThatThrownBy(() ->
                h.execute(sql))
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessage("Wahoo");

        assertThatThrownBy(() ->
                h.createBatch()
                        .add(sql)
                        .execute())
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessage("Wahoo");


        assertThatThrownBy(() ->
                h.configure(SqlStatements.class, ss -> ss.setUnusedBindingAllowed(true))
                        .prepareBatch(sql)
                        .bind("unused", 1)
                        .add()
                        .execute())
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessage("Wahoo");

        assertThatThrownBy(() ->
                h.createScript(sql)
                        .execute())
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessage("Wahoo");

        assertThatThrownBy(() ->
                h.createCall(sql)
                        .invoke())
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessage("Wahoo");
    }
}
