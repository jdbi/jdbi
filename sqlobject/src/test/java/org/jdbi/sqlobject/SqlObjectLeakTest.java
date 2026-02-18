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

import java.util.List;
import java.util.Objects;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.mapper.RowMappers;
import org.jdbi.core.mapper.reflect.ConstructorMapper;
import org.jdbi.core.statement.StatementCustomizers;
import org.jdbi.core.statement.UnableToExecuteStatementException;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SqlObjectLeakTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
            .withPlugin(new SqlObjectPlugin())
            .withInitializer(TestingInitializers.usersWithData())
            .withConfig(RowMappers.class, r -> r.register(User.class, ConstructorMapper.of(User.class)));

    @Test
    void testManagedHandleExplodingAttachedDao() {
        assertThatExceptionOfType(UnableToExecuteStatementException.class).isThrownBy(() -> {
            try (Handle handle = h2Extension.openHandle()) {
                handle.addCustomizer(StatementCustomizers.fetchSize(-1));
                UserDao handleDao = handle.attach(UserDao.class);
                handleDao.getUserNames();
            }
        });
    }

    @Test
    void testExplodingOnDemandDao() {
        assertThatExceptionOfType(UnableToExecuteStatementException.class).isThrownBy(() -> {
            Jdbi jdbi = h2Extension.getJdbi();
            jdbi.addCustomizer(StatementCustomizers.fetchSize(-1));
            UserDao jdbiDao = h2Extension.getJdbi().onDemand(UserDao.class);
            jdbiDao.getUserNames();
        });
    }

    @Test
    void testExplodingExtensionDao() {
        assertThatExceptionOfType(UnableToExecuteStatementException.class).isThrownBy(() -> {
            Jdbi jdbi = h2Extension.getJdbi();
            jdbi.addCustomizer(StatementCustomizers.fetchSize(-1));
            jdbi.withExtension(UserDao.class, UserDao::getUserNames);
        });
    }

    public static class User {

        private final int id;
        private final String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
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

    public interface UserDao extends SqlObject {

        @SqlQuery("SELECT name from users order by id")
        List<String> getUserNames();
    }
}
