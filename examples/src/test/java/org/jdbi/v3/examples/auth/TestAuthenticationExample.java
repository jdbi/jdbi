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
package org.jdbi.v3.examples.auth;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;
import org.jdbi.v3.examples.auth.AuthenticationExample.AuthContext;
import org.jdbi.v3.examples.auth.AuthenticationExample.UseAuthentication;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestAuthenticationExample {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg)
            .withInitializer((ds, h) -> {
                h.execute("CREATE TABLE users (id serial primary key, name varchar(50), password varchar(50))");
                h.execute("CREATE TABLE data (id serial primary key, value varchar(50))");
                h.execute("INSERT INTO users (name, password) VALUES ('user','correct')");
                h.execute("INSERT INTO data (value) VALUES ('secret')");
            })
            .withPlugin(new SqlObjectPlugin());

    private DataDao dao;

    @BeforeEach
    public void setUp() {
        var jdbi = pgExtension.getJdbi();
        jdbi.registerRowMapper(Data.class, ConstructorMapper.of(Data.class));

        this.dao = jdbi.onDemand(DataDao.class);
    }

    @Test
    public void authenticateSuccess() {
        var authContext = new AuthContext("user", "correct");

        var data = dao.getData(authContext, 1);
        assertThat(data).hasFieldOrPropertyWithValue("id", 1);
        assertThat(data).hasFieldOrPropertyWithValue("value", "secret");
    }


    @Test
    public void authenticateBadUser() {
        var authContext = new AuthContext("attacker", "correct");

        assertThrows(IllegalStateException.class, () -> dao.getData(authContext, 1));
    }

    @Test
    public void authenticateFailure() {
        var authContext = new AuthContext("user", "incorrect");

        assertThrows(IllegalStateException.class, () -> dao.getData(authContext, 1));
    }

    @Test
    public void missingAuth() {
        assertThrows(IllegalArgumentException.class, () -> dao.getSneakyData(1));
    }

    public static class Data {
        private final int id;
        private final String value;

        @JdbiConstructor
        public Data(int id, String value) {
            this.id = id;
            this.value = value;
        }

        public int getId() {
            return id;
        }

        public String getValue() {
            return value;
        }
    }

    @UseAuthentication
    public interface DataDao {
        @SqlQuery("SELECT * FROM data WHERE id = :id")
        Data getData(AuthContext authContext, int id);

        @SqlQuery("SELECT * FROM data WHERE id = :id")
        Data getSneakyData(int id);
    }
}
