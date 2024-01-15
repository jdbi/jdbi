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
package org.jdbi.v3.e2e.testcontainer.mysql;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Testcontainers
public class TestMySQL {

    @Container
    static JdbcDatabaseContainer<?> dbContainer = new MySQLContainer<>("mysql");

    @RegisterExtension
    JdbiExtension extension = JdbiTestcontainersExtension.instance(dbContainer)
        .withPlugin(new SqlObjectPlugin());

    @Test
    void testIssue2402() {
        final Handle handle = extension.getSharedHandle();
        handle.registerRowMapper(Contact.class, ConstructorMapper.of(Contact.class));

        handle.execute("CREATE TABLE contacts (contact_id INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT, etag VARCHAR(255))");

        for (int i = 0; i < 100; i++) {
            handle.execute(format("INSERT INTO contacts (etag) VALUES ('tag_%05d')", i));
        }

        ContactDao contactDao = handle.attach(ContactDao.class);

        List<Contact> contacts = contactDao.getAllContacts();
        assertThat(contacts).hasSize(100);

        List<String> test1 = contactDao.testOne("9");
        assertThat(test1).hasSize(10);

        List<String> test2 = contactDao.testTwo("9");
        assertThat(test2).hasSize(10);
    }

    public interface ContactDao {

        @SqlQuery("SELECT * FROM contacts")
        List<Contact> getAllContacts();

        @SqlQuery("SELECT contact_id "
            + " FROM contacts "
            + " WHERE RIGHT(etag, 1) = RIGHT(:etag, 1)")
        List<String> testOne(@Bind("etag") String etag);

        @SqlQuery("SELECT contact_id"
            + " FROM contacts "
            + " WHERE etag LIKE \\'%<etagPattern>\\'")
        List<String> testTwo(@Define("etagPattern") String etag);
    }

    public static class Contact {

        private final int id;
        private final String etag;

        public Contact(@ColumnName("contact_id") int id, String etag) {
            this.id = id;
            this.etag = etag;
        }

        public int id() {
            return id;
        }

        public String etag() {
            return etag;
        }
    }


    @Test
    public void testIssue2535PassesSingleLine() {
        String sqlScript = "CREATE PROCEDURE QWE() "
            + "BEGIN "
            + "END; "
            + "DROP PROCEDURE IF EXISTS QWE;";
        int[] result = extension.getJdbi().withHandle(h -> h.createScript(sqlScript).execute());

        assertThat(result).isEqualTo(new int[] {0, 0 });
    }

    @Test
    public void testIssue2535FailsMultiLine() {
        String sqlScript = "CREATE PROCEDURE QWE()\n"
            + "BEGIN\n"
            + "END;\n"
            + "DROP PROCEDURE IF EXISTS QWE;\n";
        int[] result = extension.getJdbi().withHandle(h -> h.createScript(sqlScript).execute());

        assertThat(result).isEqualTo(new int[] {0, 0});
    }
}
