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
package org.jdbi.core.argument;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.junit5.H2DatabaseExtension;
import org.jdbi.core.statement.PreparedBatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestArgumentFactory {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    @Test
    public void testRegisterOnJdbi() {
        final Jdbi db = h2Extension.getJdbi();
        db.registerArgument(new NameAF());
        try (Handle h = db.open()) {
            h.createUpdate("insert into something (id, name) values (:id, :name)")
                .bind("id", 7)
                .bind("name", new Name("Brian", "McCallister"))
                .execute();

            String fullName = h.createQuery("select name from something where id = 7").mapTo(String.class).one();

            assertThat(fullName).isEqualTo("Brian McCallister");
        }
    }

    @Test
    public void testRegisterOnHandle() {
        try (Handle h = h2Extension.openHandle()) {
            h.registerArgument(new NameAF());
            h.createUpdate("insert into something (id, name) values (:id, :name)")
                .bind("id", 7)
                .bind("name", new Name("Brian", "McCallister"))
                .execute();

            String fullName = h.createQuery("select name from something where id = 7").mapTo(String.class).one();

            assertThat(fullName).isEqualTo("Brian McCallister");
        }
    }

    @Test
    public void testRegisterOnStatement() {
        assertThat(h2Extension.getSharedHandle()
            .createUpdate("insert into something (id, name) values (:id, :name)")
            .registerArgument(new NameAF())
            .bind("id", 1)
            .bind("name", new Name("Brian", "McCallister"))
            .execute()).isOne();
    }

    @Test
    public void testOnPreparedBatch() {
        Handle h = h2Extension.getSharedHandle();
        PreparedBatch batch = h.prepareBatch("insert into something (id, name) values (:id, :name)");
        batch.registerArgument(new NameAF());

        batch.bind("id", 1).bind("name", new Name("Brian", "McCallister")).add();
        batch.bind("id", 2).bind("name", new Name("Henning", "S")).add();
        batch.execute();

        List<String> rs = h.createQuery("select name from something order by id")
            .mapTo(String.class)
            .list();

        assertThat(rs).containsExactly("Brian McCallister", "Henning S");
    }

    public static class NameAF implements ArgumentFactory {

        @Override
        public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
            if (expectedType == Name.class || value instanceof Name) {
                Name nameValue = (Name) value;
                return config.get(Arguments.class).findFor(String.class, nameValue.getFullName());
            }
            return Optional.empty();
        }
    }

    public static class Name {

        private final String first;
        private final String last;

        public Name(String first, String last) {
            this.first = first;
            this.last = last;
        }

        public String getFullName() {
            return first + " " + last;
        }

        @Override
        public String toString() {
            return "<Name first=" + first + " last=" + last + " >";
        }
    }

}
