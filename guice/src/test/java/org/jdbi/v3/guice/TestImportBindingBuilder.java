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

import javax.annotation.Nullable;
import javax.sql.DataSource;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.guice.TestImportBindingBuilder.TesterModule.Tester;
import org.jdbi.v3.guice.util.GuiceTestSupport;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class TestImportBindingBuilder {

    static final String UNSET = "unset";
    static final String GLOBAL_ANNOTATED = "Global, Annotated";
    static final String GLOBAL_INTERFACE_ANNOTATED = "Global Interface, Annotated";
    static final String DEFAULT = "Default";
    static final String DEFAULT_INTERFACE = "Default Interface";

    private Module dataModule = null;

    private final Annotation a = Names.named("a-test");
    private final Annotation b = Names.named("b-test");

    @Before
    public void setUp() {
        dataModule = binder -> {
            binder.bind(DataSource.class).annotatedWith(a).toInstance(new JdbcDataSource());
            binder.bind(TestObject.class).annotatedWith(a).toInstance(new TestObject(GLOBAL_ANNOTATED));
            binder.bind(TestInterface.class).annotatedWith(a).toInstance(new TestObject(GLOBAL_INTERFACE_ANNOTATED));

            binder.bind(DataSource.class).annotatedWith(b).toInstance(new JdbcDataSource());
        };
    }

    @Test
    public void testStaticBindings() {
        Module testModule = new AbstractJdbiDefinitionModule(a) {
            @Override
            public void configureJdbi() {
                // tester code
                bind(Tester.class).toInstance(new Tester());
                expose(Tester.class);

                importBinding(TestObject.class);
                importBinding(TestInterface.class);
            }
        };

        Injector inj = GuiceTestSupport.createTestInjector(testModule, dataModule);
        inj.getInstance(Tester.class).test(GLOBAL_ANNOTATED, GLOBAL_INTERFACE_ANNOTATED);

    }

    @Test
    public void testStaticBindingToType() {
        Module testModule = new AbstractJdbiDefinitionModule(a) {
            @Override
            public void configureJdbi() {
                // tester code
                bind(Tester.class).toInstance(new Tester());
                expose(Tester.class);

                importBinding(TestInterface.class).to(TestObject.class);
            }
        };

        Injector inj = GuiceTestSupport.createTestInjector(testModule, dataModule);
        inj.getInstance(Tester.class).test(UNSET, GLOBAL_ANNOTATED);
    }

    @Test
    public void testStaticBindingsTypeLiterals() {
        Module testModule = new AbstractJdbiDefinitionModule(a) {
            @Override
            public void configureJdbi() {
                // tester code
                bind(Tester.class).toInstance(new Tester());
                expose(Tester.class);

                importBinding(TestObject.class).to(new TypeLiteral<TestObject>() {}).in(Scopes.SINGLETON);
                importBinding(new TypeLiteral<TestInterface>() {}).to(TestInterface.class).in(Scopes.SINGLETON);
            }
        };

        Injector inj = GuiceTestSupport.createTestInjector(testModule, dataModule);
        inj.getInstance(Tester.class).test(GLOBAL_ANNOTATED, GLOBAL_INTERFACE_ANNOTATED);
    }

    @Test
    public void testBindings() {
        Module testModule = new AbstractJdbiDefinitionModule(a) {
            @Override
            public void configureJdbi() {
                // tester code
                bind(Tester.class).toInstance(new Tester());
                expose(Tester.class);

                importBindingLoosely(TestObject.class);
                importBindingLoosely(TestInterface.class);
            }
        };

        Injector inj = GuiceTestSupport.createTestInjector(testModule, dataModule);
        inj.getInstance(Tester.class).test(GLOBAL_ANNOTATED, GLOBAL_INTERFACE_ANNOTATED);

    }

    @Test
    public void testBindingToType() {
        Module testModule = new AbstractJdbiDefinitionModule(a) {
            @Override
            public void configureJdbi() {
                // tester code
                bind(Tester.class).toInstance(new Tester());
                expose(Tester.class);

                importBindingLoosely(TestInterface.class).to(TestObject.class);
            }
        };

        Injector inj = GuiceTestSupport.createTestInjector(testModule, dataModule);
        inj.getInstance(Tester.class).test(UNSET, GLOBAL_ANNOTATED);
    }

    @Test
    public void testBindingsTypeLiterals() {
        Module testModule = new AbstractJdbiDefinitionModule(a) {
            @Override
            public void configureJdbi() {
                // tester code
                bind(Tester.class).toInstance(new Tester());
                expose(Tester.class);

                importBindingLoosely(TestObject.class).to(new TypeLiteral<TestObject>() {}).in(Scopes.SINGLETON);
                importBindingLoosely(new TypeLiteral<TestInterface>() {}).to(TestInterface.class).in(Scopes.SINGLETON);
            }
        };

        Injector inj = GuiceTestSupport.createTestInjector(testModule, dataModule);
        inj.getInstance(Tester.class).test(GLOBAL_ANNOTATED, GLOBAL_INTERFACE_ANNOTATED);
    }

    @Test
    public void testBindingsAbsent() {
        Module testModule = new AbstractJdbiDefinitionModule(b) {
            @Override
            public void configureJdbi() {
                // tester code
                bind(Tester.class).toInstance(new Tester());
                expose(Tester.class);

                importBindingLoosely(TestObject.class);
                importBindingLoosely(TestInterface.class);
            }
        };

        Injector inj = GuiceTestSupport.createTestInjector(testModule, dataModule);
        inj.getInstance(Tester.class).test(null, null);

    }

    @Test
    public void testBindingToTypeAbsent() {
        Module testModule = new AbstractJdbiDefinitionModule(b) {
            @Override
            public void configureJdbi() {
                // tester code
                bind(Tester.class).toInstance(new Tester());
                expose(Tester.class);

                importBindingLoosely(TestInterface.class).to(TestObject.class);
            }
        };

        Injector inj = GuiceTestSupport.createTestInjector(testModule, dataModule);
        inj.getInstance(Tester.class).test(UNSET, null);
    }

    @Test
    public void testBindingsTypeLiteralsAbsent() {
        Module testModule = new AbstractJdbiDefinitionModule(b) {
            @Override
            public void configureJdbi() {
                // tester code
                bind(Tester.class).toInstance(new Tester());
                expose(Tester.class);

                importBindingLoosely(TestObject.class).to(new TypeLiteral<TestObject>() {}).in(Scopes.SINGLETON);
                importBindingLoosely(new TypeLiteral<TestInterface>() {}).to(TestInterface.class).in(Scopes.SINGLETON);
            }
        };

        Injector inj = GuiceTestSupport.createTestInjector(testModule, dataModule);
        inj.getInstance(Tester.class).test(null, null);
    }

    @Test
    public void testBindingsDefault() {
        Module testModule = new AbstractJdbiDefinitionModule(b) {
            @Override
            public void configureJdbi() {
                // tester code
                bind(Tester.class).toInstance(new Tester());
                expose(Tester.class);

                importBindingLoosely(TestObject.class).withDefault(new TestObject(DEFAULT));
                importBindingLoosely(TestInterface.class).withDefault(new TestObject(DEFAULT_INTERFACE));
            }
        };

        Injector inj = GuiceTestSupport.createTestInjector(testModule, dataModule);
        inj.getInstance(Tester.class).test(DEFAULT, DEFAULT_INTERFACE);

    }

    @Test
    public void testBindingToTypeDefault() {
        Module testModule = new AbstractJdbiDefinitionModule(b) {
            @Override
            public void configureJdbi() {
                // tester code
                bind(Tester.class).toInstance(new Tester());
                expose(Tester.class);

                importBindingLoosely(TestInterface.class)
                    .withDefault(() -> DEFAULT_INTERFACE)
                    .to(TestObject.class);
            }
        };

        Injector inj = GuiceTestSupport.createTestInjector(testModule, dataModule);
        inj.getInstance(Tester.class).test(UNSET, DEFAULT_INTERFACE);
    }

    @Test
    public void testBindingsTypeLiteralsDefault() {
        Module testModule = new AbstractJdbiDefinitionModule(b) {
            @Override
            public void configureJdbi() {
                // tester code
                bind(Tester.class).toInstance(new Tester());
                expose(Tester.class);

                importBindingLoosely(TestObject.class)
                    .withDefault(new TestObject(DEFAULT))
                    .to(new TypeLiteral<TestObject>() {})
                    .in(Scopes.SINGLETON);
                importBindingLoosely(new TypeLiteral<TestInterface>() {})
                    .withDefault(new TestObject(DEFAULT_INTERFACE))
                    .to(TestInterface.class)
                    .in(Scopes.SINGLETON);
            }
        };

        Injector inj = GuiceTestSupport.createTestInjector(testModule, dataModule);
        inj.getInstance(Tester.class).test(DEFAULT, DEFAULT_INTERFACE);
    }

    @FunctionalInterface
    public interface TestInterface {

        String getValue();
    }

    public static class TestObject implements TestInterface {

        private final String value;

        @Inject
        public TestObject(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    public static class TesterModule extends PrivateModule {

        @Override
        protected void configure() {
            bind(Tester.class).toInstance(new Tester());
            expose(Tester.class);
        }

        public static class Tester {

            private TestObject testObject = new TestObject(UNSET);
            private TestInterface testInterface = new TestObject(UNSET);

            @Inject(optional = true)
            public void setTestObject(@Nullable TestObject testObject) {
                this.testObject = testObject;
            }

            @Inject(optional = true)
            public void setTestInterface(@Nullable TestInterface testInterface) {
                this.testInterface = testInterface;
            }

            public void test(String objectString, String interfaceString) {
                if (objectString == null) {
                    assertNull(testObject);
                } else {
                    if (testObject == null) {
                        fail("test object is null");
                    }
                    assertEquals(objectString, testObject.getValue());
                }

                if (interfaceString == null) {
                    assertNull(testInterface);
                } else {
                    if (testInterface == null) {
                        fail("test interface is null");
                    }
                    assertEquals(interfaceString, testInterface.getValue());
                }
            }
        }
    }
}
