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

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jdbi.core.mapper.RowMappers;
import org.jdbi.core.mapper.reflect.ConstructorMapper;
import org.jdbi.core.result.ResultIterator;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An extension object that is attached to a handle can return iterators or streams. This class tests various ways on how this may leak resources.
 */
public class AttachedHandleLeakTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
            .withPlugin(new SqlObjectPlugin())
            .withInitializer(TestingInitializers.usersWithData())
            .withConfig(RowMappers.class, r -> r.register(User.class, ConstructorMapper.of(User.class)));

    private UserDao dao;

    @BeforeEach
    public void setUp() {
        this.dao = h2Extension.getSharedHandle().attach(UserDao.class);
    }

    @Test
    public void testLeakDoNothing() {
        for (int i = 0; i < 1000; i++) {
            Iterator<User> it = dao.getIterableUsers();
            assertThat(it.next()).extracting("id").isEqualTo(1);
        }
    }

    @Test
    public void testLeakCallClean() throws Exception {
        for (int i = 0; i < 1000; i++) {
            Iterator<User> it = dao.getIterableUsers();
            assertThat(it.next()).extracting("id").isEqualTo(1);
            dao.getHandle().clean();
        }
    }

    @Test
    public void testLeakTwr() {
        for (int i = 0; i < 1000; i++) {
            try (ResultIterator<User> it = dao.getIterableUsers()) {
                assertThat(it.next()).extracting("id").isEqualTo(1);
            }
        }
    }

    @Test
    public void testLeakStreamDoNothing() {
        for (int i = 0; i < 1000; i++) {
            Stream<User> stream = dao.getStreamingUsers();
            assertThat(stream.findFirst()).containsInstanceOf(User.class);
        }
    }

    @Test
    public void testLeakStreamCallClean() throws Exception {
        for (int i = 0; i < 1000; i++) {
            Stream<User> stream = dao.getStreamingUsers();
            assertThat(stream.findFirst()).containsInstanceOf(User.class);
            dao.getHandle().clean();
        }
    }

    @Test
    public void testLeakStreamTwr() {
        for (int i = 0; i < 1000; i++) {
            try (Stream<User> stream = dao.getStreamingUsers()) {
                assertThat(stream.findFirst()).containsInstanceOf(User.class);
            }
        }
    }

    @Test
    public void testLeakConsumer() {
        for (int i = 0; i < 1000; i++) {
            dao.getIterableUsers(it -> assertThat(it.next()).extracting("id").isEqualTo(1));
        }
    }

    @Test
    public void testLeakStreamConsumer() {
        for (int i = 0; i < 1000; i++) {
            dao.getStreamableUsers(stream -> assertThat(stream.findFirst()).containsInstanceOf(User.class));
        }
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

        @SqlQuery("SELECT * from users order by id")
        ResultIterator<User> getIterableUsers();

        @SqlQuery("SELECT * from users order by id")
        Stream<User> getStreamingUsers();

        @SqlQuery("SELECT * from users order by id")
        void getIterableUsers(Consumer<Iterator<User>> consumer);

        @SqlQuery("SELECT * from users order by id")
        void getStreamableUsers(Consumer<Stream<User>> consumer);
    }
}
