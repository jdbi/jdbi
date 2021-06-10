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

import javax.inject.Inject;
import javax.sql.DataSource;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.opentable.db.postgres.embedded.PreparedDbProvider;
import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class TestMultipleModules {

    @Inject
    @Named("db1")
    public Jdbi jdbiDb1 = null;

    @Inject
    @Named("db2")
    public Jdbi jdbiDb2 = null;

    @Before
    public void setUp() throws Exception {

        PreparedDbProvider provider = PreparedDbProvider.forPreparer(p -> {});
        DataSource ds1 = provider.createDataSource();
        DataSource ds2 = provider.createDataSource();

        assertNotEquals(ds1, ds2);

        Annotation db1Annotation = Names.named("db1");
        Module db1Module = new AbstractJdbiModule(db1Annotation) {
            @Override
            protected void configureJdbi() {}
        };

        Annotation db2Annotation = Names.named("db2");
        Module db2Module = new AbstractJdbiModule(db2Annotation) {
            @Override
            protected void configureJdbi() {}
        };

        Injector inj = Guice.createInjector(Stage.PRODUCTION,
            db1Module,
            db2Module,
            binder -> binder.bind(DataSource.class).annotatedWith(db1Annotation).toInstance(ds1),
            binder -> binder.bind(DataSource.class).annotatedWith(db2Annotation).toInstance(ds2),
            Binder::disableCircularProxies,
            Binder::requireExplicitBindings,
            Binder::requireExactBindingAnnotations,
            Binder::requireAtInjectOnConstructors);

        inj.injectMembers(this);
    }

    @Test
    public void testMultiJdbi() {
        assertNotNull(jdbiDb1);
        assertNotNull(jdbiDb2);
        assertNotEquals(jdbiDb1, jdbiDb2);
    }
}
