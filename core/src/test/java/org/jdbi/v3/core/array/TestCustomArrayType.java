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
package org.jdbi.v3.core.array;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCustomArrayType {

    private void init(Jdbi db) {
        db.registerArrayType(UserId.class, "int", UserId::getId);
        db.registerColumnMapper(new UserIdColumnMapper());
    }

    /**
     * Test binding and mapping a custom array type; binding requires registration
     * of a {@link SqlArrayType} implementation, and mapping requires registration
     * of a regular {@link ColumnMapper} for the element
     * type
     */
    @Test
    public void testCustomArrayType() {
        Jdbi db = Jdbi.create("jdbc:hsqldb:mem:" + UUID.randomUUID());
        init(db);

        UserId user1 = new UserId(10);
        UserId user2 = new UserId(12);
        try (Handle handle = db.open()) {
            handle.execute("create table groups ("
                + "id int primary key, "
                + "user_ids int array)");
            assertThat(
                handle.createUpdate("insert into groups (id, user_ids) values (?,?)")
                    .bind(0, 1)
                    .bind(1, new UserId[]{user1, user2})
                    .execute())
                .isEqualTo(1);

            final List<UserId> result = handle.createQuery("select user_ids from groups where id = ?")
                .bind(0, 1)
                .mapTo(new GenericType<List<UserId>>() {})
                .one();

            assertThat(result).containsExactly(user1, user2);
        }
    }

    static class UserId {
        private final int id;

        UserId(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UserId userId = (UserId) o;
            return id == userId.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    static class UserIdColumnMapper implements ColumnMapper<UserId> {

        @Override
        public UserId map(ResultSet rs, int col, StatementContext ctx) throws SQLException {
            return new UserId(rs.getInt(col));
        }
    }
}
