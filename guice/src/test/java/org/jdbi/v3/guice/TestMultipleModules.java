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

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.guice.util.GuiceTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMultipleModules {

    @Inject
    @Named("db1")
    public Jdbi jdbiDb1 = null;

    @Inject
    @Named("db2")
    public Jdbi jdbiDb2 = null;

    @BeforeEach
    public void setUp() throws Exception {
        DataSource ds1 = new JdbcDataSource();
        DataSource ds2 = new JdbcDataSource();

        assertThat(ds1).isNotEqualTo(ds2);

        Annotation db1Annotation = Names.named("db1");
        Module db1Module = new AbstractJdbiDefinitionModule(db1Annotation) {
            @Override
            public void configureJdbi() {}
        };

        Annotation db2Annotation = Names.named("db2");
        Module db2Module = new AbstractJdbiDefinitionModule(db2Annotation) {
            @Override
            public void configureJdbi() {}
        };

        Injector inj = GuiceTestSupport.createTestInjector(
            db1Module,
            db2Module,
            binder -> binder.bind(DataSource.class).annotatedWith(db1Annotation).toInstance(ds1),
            binder -> binder.bind(DataSource.class).annotatedWith(db2Annotation).toInstance(ds2));

        inj.injectMembers(this);
    }

    @Test
    public void testMultiJdbi() {
        assertThat(jdbiDb2).isNotNull();
        assertThat(jdbiDb1)
                .isNotNull()
                .isNotEqualTo(jdbiDb2);
    }
}
