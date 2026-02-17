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

import org.jdbi.core.Something;
import org.jdbi.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRegisterConstructorMapper {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Dao dao;

    @BeforeEach
    public void setUp() {
        dao = h2Extension.getSharedHandle().attach(Dao.class);
    }

    @Test
    public void testMapperRegistered() {
        dao.insert(1, "brain");
        Something brain = dao.getById(1);
        assertThat(brain.getId()).isOne();
        assertThat(brain.getName()).isEqualTo("brain");
    }

    @Test
    public void testStaticFactoryMapperRegistered() {
        dao.insert(1, "brain");
        Something brain = dao.getByIdUsingFactoryMethod(1);
        assertThat(brain.getId()).isOne();
        assertThat(brain.getName()).isEqualTo("brain");
    }

    @Test
    public void testMapperPrefixed() {
        dao.insert(1, "brain");
        Something brain = dao.getByIdPrefixed(1);
        assertThat(brain.getId()).isOne();
        assertThat(brain.getName()).isEqualTo("brain");
    }

    // subclassing just to hide Something() constructor
    public static class SubSomething extends Something {
        public SubSomething(int id, String name) {
            super(id, name);
        }
    }

    public static class FactoryClass {
        @SuppressWarnings("unused")
        public static Something createSomething(int id, String name) {
            return new SubSomething(id, name);
        }
    }

    public interface Dao {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id=:id")
        @RegisterConstructorMapper(SubSomething.class)
        SubSomething getById(@Bind("id") int id);

        @SqlQuery("select id, name from something where id=:id")
        @RegisterConstructorMapper(value = Something.class, usingStaticMethodIn = FactoryClass.class)
        Something getByIdUsingFactoryMethod(@Bind("id") int id);

        @SqlQuery("select id thing_id, name thing_name from something where id=:id")
        @RegisterConstructorMapper(value = SubSomething.class, prefix = "thing_")
        SubSomething getByIdPrefixed(@Bind("id") int id);
    }
}
