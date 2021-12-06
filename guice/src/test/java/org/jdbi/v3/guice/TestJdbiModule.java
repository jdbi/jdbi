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
package org.jdbi.v3.guice;

import java.lang.annotation.Annotation;
import java.util.UUID;

import javax.sql.DataSource;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.guice.util.GuiceTestSupport;
import org.jdbi.v3.guice.util.MyString;
import org.jdbi.v3.guice.util.MyString.MyStringColumnMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestJdbiModule {

    @RegisterExtension
    public EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults()
        .withDatabasePreparer(ds -> GuiceTestSupport.executeSql(ds,
            "DROP TABLE IF EXISTS arrays",
            "CREATE TABLE arrays (u UUID, i INT[])"
        )).build();

    @Inject
    @Named("test")
    public Jdbi jdbi = null;

    @BeforeEach
    public void setUp() throws Exception {
        Annotation a = Names.named("test");
        Module testModule = new AbstractJdbiDefinitionModule(a) {
            @Override
            public void configureJdbi() {
                bindPlugin().toInstance(new GuavaPlugin());

                bindColumnMapper().to(MyStringColumnMapper.class).in(Scopes.SINGLETON);
            }
        };

        DataSource ds = pg.createDataSource();
        Injector inj = GuiceTestSupport.createTestInjector(
            binder -> binder.bind(DataSource.class).annotatedWith(a).toInstance(ds),
            testModule);

        inj.injectMembers(this);

        assertNotNull(jdbi);
    }

    @Test
    public void testMyString() {

        UUID testUuid = UUID.randomUUID();

        jdbi.withHandle(h -> {
            h.execute("INSERT INTO arrays (u) VALUES(?)", testUuid);
            MyString result = h.createQuery("SELECT u FROM arrays")
                .mapTo(MyString.class)
                .one();
            assertThat(result.getValue()).isEqualTo(testUuid.toString());
            return null;
        });
    }

    @Test
    public void testGuavaPlugin() {
        Integer[] testInts = new Integer[]{
            5, 4, -6, 1, 9, Integer.MAX_VALUE, Integer.MIN_VALUE
        };

        jdbi.withHandle(h -> {
            h.execute("INSERT INTO arrays (i) VALUES(?)", (Object) testInts);
            ImmutableList<Integer> list = h.createQuery("SELECT i FROM arrays")
                .mapTo(new GenericType<ImmutableList<Integer>>() {})
                .one();
            assertThat(list).contains(testInts);
            return null;
        });
    }
}
