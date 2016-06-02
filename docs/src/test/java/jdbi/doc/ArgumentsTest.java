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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Handle;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.argument.Argument;
import org.jdbi.v3.argument.ArgumentFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ArgumentsTest {

    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();
    private Handle handle;

    @Before
    public void getHandle() {
        handle = db.getSharedHandle();
    }

    @Test
    // tag::bindValue[]
    public void bindInt() {
        assertEquals(3, handle.createQuery("SELECT :id")
            .bind("id", 3)
            .mapTo(Integer.class)
            .findOnly()
            .intValue());
    }
    // end::bindValue[]

    // tag::uuidArgument[]
    static class UUIDArgument implements Argument {
        private UUID uuid;

        public UUIDArgument(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public void apply(int position, PreparedStatement statement, StatementContext ctx)
        throws SQLException {
            statement.setString(position, uuid.toString()); // <1>
        }
    }

    @Test
    public void uuidArgument() {
        UUID u = UUID.randomUUID();
        assertEquals(u.toString(), handle.createQuery("SELECT CAST(:uuid AS VARCHAR)")
            .bind("uuid", new UUIDArgument(u))
            .mapTo(String.class)
            .findOnly());
    }
    // end::uuidArgument[]

    // tag::uuidArgumentFactory[]
    static class UUIDArgumentFactory implements ArgumentFactory {
        @Override
        public Optional<Argument> build(Type type, Object value, StatementContext ctx) {
            return type == UUID.class ?
                    Optional.of(new UUIDArgument((UUID) value)) :
                        Optional.empty();
        }
    }

    @Test
    public void uuidArgumentFactory() {
        UUID u = UUID.randomUUID();
        handle.registerArgumentFactory(new UUIDArgumentFactory());
        assertEquals(u.toString(), handle.createQuery("SELECT CAST(:uuid AS VARCHAR)")
            .bind("uuid", u)
            .mapTo(String.class)
            .findOnly());
    }
    // end::uuidArgumentFactory[]
}
