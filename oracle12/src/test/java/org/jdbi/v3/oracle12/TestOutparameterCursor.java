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
package org.jdbi.v3.oracle12;

import java.sql.Types;
import java.util.List;
import java.util.Objects;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.Call;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.tc.JdbiTestcontainersExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
@Testcontainers
public class TestOutparameterCursor {

    static final String CONTAINER_VERSION = "gvenzl/oracle-free:" + System.getProperty("oracle.container.version", "slim-faststart");

    @Container
    static OracleContainer oc = new OracleContainer(CONTAINER_VERSION);

    @RegisterExtension
    JdbiExtension oracleExtension = JdbiTestcontainersExtension.instance(oc)
            .withInitializer((ds, h) -> {
                h.execute("CREATE TABLE USERS (ID INTEGER, NAME VARCHAR(255))");
                h.execute("INSERT INTO USERS VALUES (1, 'Alice')");
                h.execute("INSERT INTO USERS VALUES (2, 'Bob')");
                h.execute("CREATE OR REPLACE PROCEDURE get_user_by_name(\n"
                        + "p_name IN USERS.NAME%TYPE,\n"
                        + "o_c_dbuser OUT SYS_REFCURSOR) "
                        + "AS\n"
                        + "BEGIN\n"
                        + "OPEN o_c_dbuser FOR\n"
                        + "SELECT * FROM USERS WHERE NAME LIKE p_name || '%';\n"
                        + "END;");
            });

    @Test
    public void someTest() {
        RowMapper<User> userMapper = ConstructorMapper.of(User.class);

        try (Call call = oracleExtension.getSharedHandle().createCall("call get_user_by_name(:a,:b)")
                .bind("a", "Alice")
                .registerOutParameter("b", Types.REF_CURSOR)) {
            List<User> result = call.invoke(outParameters -> {
                return outParameters.getRowSet("b").map(userMapper).list();
            });

            assertThat(result).isNotNull().hasSize(1).contains(new User(1, "Alice"));
        }
    }

    public static class User {

        private final int id;
        private final String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            User user = (User) o;
            return id == user.id && Objects.equals(name, user.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }
}
