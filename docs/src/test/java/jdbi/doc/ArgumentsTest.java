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
package jdbi.doc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class ArgumentsTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2();

    private Handle handle;

    @BeforeEach
    public void getHandle() {
        handle = h2Extension.getSharedHandle();
    }

    @Test
    // tag::bindValue[]
    public void bindInt() {
        assertThat(handle.createQuery("SELECT :id")
            .bind("id", 3)
            .mapTo(Integer.class)
            .one()
            .intValue()).isEqualTo(3);
    }
    // end::bindValue[]

    // tag::uuidArgument[]
    static class UUIDArgument implements Argument {
        private UUID uuid;

        UUIDArgument(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
            statement.setString(position, uuid.toString()); // <1>
        }
    }

    @Test
    public void uuidArgument() {
        UUID u = UUID.randomUUID();
        assertThat(handle.createQuery("SELECT CAST(:uuid AS VARCHAR)")
            .bind("uuid", new UUIDArgument(u))
            .mapTo(String.class)
            .one()).isEqualTo(u.toString());
    }
    // end::uuidArgument[]

    // tag::uuidArgumentFactory[]
    static class UUIDArgumentFactory extends AbstractArgumentFactory<UUID> {
        UUIDArgumentFactory() {
            super(Types.VARCHAR); // <1>
        }

        @Override
        protected Argument build(UUID value, ConfigRegistry config) {
            return (position, statement, ctx) -> statement.setString(position, value.toString()); // <2>
        }
    }

    @Test
    public void uuidArgumentFactory() {
        UUID u = UUID.randomUUID();
        handle.registerArgument(new UUIDArgumentFactory());
        assertThat(handle.createQuery("SELECT CAST(:uuid AS VARCHAR)")
            .bind("uuid", u)
            .mapTo(String.class)
            .one()).isEqualTo(u.toString());
    }
    // end::uuidArgumentFactory[]
}
