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

import java.util.List;

import org.jdbi.core.Jdbi;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.customizer.Define;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeDefineTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
            .withInitializer(TestingInitializers.usersWithData())
            .withPlugin(new SqlObjectPlugin());

    // tag::dao[]
    interface UserDao {

        @SqlQuery("SELECT name FROM <table>") // <1>
        List<String> getNames(@Define("table") String tableName); // <2>
    }
    // end::dao[]

    interface TestDao {

        @SqlQuery("SELECT name FROM users")
        List<String> getNames();
    }

    private Jdbi jdbi;
    private List<String> names;

    @BeforeEach
    void setUp() {
        this.jdbi = h2Extension.getJdbi();
        this.names = this.jdbi.withExtension(TestDao.class, TestDao::getNames);
    }

    // tag::define[]
    @Test
    void testNames() {
        List<String> result = jdbi.withExtension(UserDao.class, dao -> dao.getNames("users")); // <3>
        assertThat(result).containsAll(names);
    }
    // end::define[]
}
