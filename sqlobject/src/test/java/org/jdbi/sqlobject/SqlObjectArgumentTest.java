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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jdbi.core.Handle;
import org.jdbi.core.argument.Argument;
import org.jdbi.core.statement.Query;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlObjectArgumentTest {

    private static final String INSERT_QUERY
        = "INSERT INTO something (id, integerValue)"
        + "\n VALUES (:id, :status)";

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    @Test
    public void testInsertClass() {
        final Handle h = h2Extension.getSharedHandle();
        final StatusDao dao = h.attach(StatusDao.class);

        // test Argument
        int count = h.createUpdate(INSERT_QUERY)
            .bind("id", 11)
            .bind("status", StatusClass.ONLINE)
                .execute();
        assertThat(count).isOne();

        // test SQL Object
        count = dao.insert(12, StatusClass.ONLINE.code);
        assertThat(count).isOne();

        // test both together
        count = dao.insert(13, StatusClass.ONLINE);
        assertThat(count).isOne();

        final Query query = h.createQuery(
                "SELECT integerValue FROM something WHERE id = 13");
        final int code = query.mapTo(int.class).one();
        assertThat(code).isEqualTo(StatusClass.ONLINE.code);
    }

    @Test
    public void testInsertEnum() {
        final Handle h = h2Extension.getSharedHandle();
        final StatusDao dao = h.attach(StatusDao.class);

        // test Argument
        int count = h.createUpdate(INSERT_QUERY)
                .bind("id", 21)
                .bind("status", StatusEnum.ONLINE)
                .execute();
        assertThat(count).isOne();

        // test SQL Object
        count = dao.insert(22, StatusEnum.ONLINE.code);
        assertThat(count).isOne();

        // test both together
        count = h.attach(StatusDao.class).insert(23, StatusEnum.ONLINE);
        assertThat(count).isOne();

        final Query query = h.createQuery(
                "SELECT integerValue FROM something WHERE id = 23");
        final int code = query.mapTo(int.class).one();
        assertThat(code).isEqualTo(StatusEnum.ONLINE.code);
    }

    public static class StatusClass implements Argument {
        public static final StatusClass
                ONLINE = new StatusClass(111),
                OFFLINE = new StatusClass(110);

        private final int code;

        private StatusClass(final int code) {
            this.code = code;
        }

        @Override
        public void apply(
                final int position,
                final PreparedStatement statement,
                final StatementContext ctx)
                throws SQLException {
            statement.setInt(position, code);
        }
    }

    public enum StatusEnum implements Argument {
        ONLINE(121),
        OFFLINE(120);

        private final int code;

        StatusEnum(final int code) {
            this.code = code;
        }

        @Override
        public void apply(
                final int position,
                final PreparedStatement statement,
                final StatementContext ctx)
                throws SQLException {
            statement.setInt(position, code);
        }
    }

    public interface StatusDao {
        @SqlUpdate(INSERT_QUERY)
        int insert(int id, int status);

        @SqlUpdate(INSERT_QUERY)
        int insert(int id, StatusClass status);

        @SqlUpdate(INSERT_QUERY)
        int insert(int id, StatusEnum status);
    }
}
