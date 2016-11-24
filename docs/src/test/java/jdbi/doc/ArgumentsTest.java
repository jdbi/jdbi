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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.jdbi.v3.core.ConfigRegistry;
import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
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
        assertThat(handle.createQuery("SELECT :id")
            .bind("id", 3)
            .mapTo(Integer.class)
            .findOnly()
            .intValue()).isEqualTo(3);
    }
    // end::bindValue[]

    // tag::uuidArgument[]
    static class UUIDArgument implements Argument<UUID> {
        @Override
        public void apply(PreparedStatement statement, int position, UUID value, StatementContext ctx)
        throws SQLException {
            statement.setString(position, value.toString()); // <1>
        }
    }

    @Test
    public void uuidArgument() {
        UUID u = UUID.randomUUID();
        handle.registerArgument(new UUIDArgument());
        assertThat(handle.createQuery("SELECT CAST(:uuid AS VARCHAR)")
            .bind("uuid", u)
            .mapTo(String.class)
            .findOnly()).isEqualTo(u.toString());
    }
    // end::uuidArgument[]

    // tag::uuidArgumentFactory[]
    static class UUIDArgumentFactory implements ArgumentFactory {
        @Override
        public Optional<Argument> build(Type type, ConfigRegistry config) {
            return type == UUID.class ?
                    Optional.of(new UUIDArgument()) :
                    Optional.empty();
        }
    }

    @Test
    public void uuidArgumentFactory() {
        UUID u = UUID.randomUUID();
        handle.registerArgument(new UUIDArgumentFactory());
        assertThat(handle.createQuery("SELECT CAST(:uuid AS VARCHAR)")
            .bind("uuid", u)
            .mapTo(String.class)
            .findOnly()).isEqualTo(u.toString());
    }
    // end::uuidArgumentFactory[]
}
