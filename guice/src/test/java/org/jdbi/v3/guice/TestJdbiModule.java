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
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.opentable.db.postgres.junit.EmbeddedPostgresRules;
import com.opentable.db.postgres.junit.SingleInstancePostgresRule;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.guice.MyString.MyStringColumnMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

public class TestJdbiModule {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @Inject
    @Named("test")
    public Jdbi jdbi = null;

    @Before
    public void setUp() {
        Annotation a = Names.named("test");
        Module testModule = new AbstractJdbiModule(a) {
            @Override
            protected void configureJdbi() {
                bindPlugin().toInstance(new GuavaPlugin());

                bindColumnMapper().to(MyStringColumnMapper.class).in(Scopes.SINGLETON);
            }
        };

        Injector inj = Guice.createInjector(Stage.PRODUCTION,
            binder -> binder.bind(DataSource.class).annotatedWith(a).toInstance(pg.getEmbeddedPostgres().getPostgresDatabase()),
            testModule,
            Binder::disableCircularProxies,
            Binder::requireExplicitBindings,
            Binder::requireExactBindingAnnotations,
            Binder::requireAtInjectOnConstructors);

        inj.injectMembers(this);

        assertNotNull(jdbi);
        createDb();
    }

    private void createDb() {
        jdbi.inTransaction(h -> {
            h.execute("DROP TABLE IF EXISTS arrays");
            h.execute("CREATE TABLE arrays (u UUID, i INT[])");
            return null;
        });
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
