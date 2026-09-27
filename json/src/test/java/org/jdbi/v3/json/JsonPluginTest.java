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
package org.jdbi.v3.json;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifier;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonPluginTest {
    private static final QualifiedType<Foo> FLAVORED_JSON_FOO = QualifiedType.of(Foo.class).with(Json.class, Flavored.class);

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new JsonPlugin());

    @BeforeEach
    public void before() {
        h2Extension.getJdbi().useHandle(h -> h.createUpdate("create table foo(bar varchar)").execute());
    }

    @Test
    public void factoryChainWorks() {
        Jdbi jdbi = h2Extension.getJdbi();
        Object instance = new Foo();
        String json = "foo";

        jdbi.getConfig(JsonConfig.class).setJsonMapper(new JsonMapper() {
            @Override
            public TypedJsonMapper forType(Type type, ConfigRegistry config) {
                assertThat(type).isEqualTo(Foo.class);
                return new TypedJsonMapper() {
                    @Override
                    public String toJson(Object value, ConfigRegistry config) {
                        assertThat(value).isEqualTo(instance);
                        return json;
                    }

                    @Override
                    public Object fromJson(String readJson, ConfigRegistry config) {
                        assertThat(readJson).isEqualTo(json);
                        return instance;
                    }
                };
            }
        });

        Object result = h2Extension.getJdbi().withHandle(h -> {
            h.createUpdate("insert into foo(bar) values(:foo)")
                .bindByType("foo", instance, QualifiedType.of(Foo.class).with(Json.class))
                .execute();

            assertThat(h.createQuery("select bar from foo").mapTo(String.class).one())
                .isEqualTo(json);

            return h.createQuery("select bar from foo")
                .mapTo(QualifiedType.of(Foo.class).with(Json.class))
                .one();
        });

        assertThat(result).isSameAs(instance);
    }

    @Test
    public void additionalQualifierUsesJsonMapper() {
        Foo instance = new Foo();
        h2Extension.getJdbi().getConfig(JsonConfig.class).setJsonMapper(new FixedJsonMapper(instance, "foo"));

        assertThat(roundTrip(instance, FLAVORED_JSON_FOO)).isSameAs(instance);
    }

    @Test
    public void additionalQualifierReachesJsonMapper() {
        Foo instance = new Foo();
        QualifierRecordingJsonMapper mapper = new QualifierRecordingJsonMapper(instance, "foo");
        h2Extension.getJdbi().getConfig(JsonConfig.class).setJsonMapper(mapper);

        assertThat(roundTrip(instance, FLAVORED_JSON_FOO)).isSameAs(instance);
        assertThat(mapper.boundTypes).containsExactly(FLAVORED_JSON_FOO);
        assertThat(mapper.mappedTypes).containsExactly(FLAVORED_JSON_FOO);
    }

    @Test
    public void qualifierWithoutJsonDoesNotUseJsonMapper() {
        Foo instance = new Foo();
        QualifiedType<Foo> flavoredFoo = QualifiedType.of(Foo.class).with(Flavored.class);
        h2Extension.getJdbi().getConfig(JsonConfig.class).setJsonMapper(new FixedJsonMapper(instance, "foo"));

        h2Extension.getJdbi().useHandle(h -> {
            assertThatThrownBy(() -> h.createUpdate("insert into foo(bar) values(:foo)")
                    .bindByType("foo", instance, flavoredFoo)
                    .execute())
                .isInstanceOf(UnableToCreateStatementException.class)
                .hasMessageContaining("No argument factory registered");

            h.execute("insert into foo(bar) values('foo')");

            assertThatThrownBy(() -> h.createQuery("select bar from foo").mapTo(flavoredFoo).one())
                .isInstanceOf(NoSuchMapperException.class);
        });
    }

    private Foo roundTrip(Foo instance, QualifiedType<Foo> type) {
        return h2Extension.getJdbi().withHandle(h -> {
            h.createUpdate("insert into foo(bar) values(:foo)")
                .bindByType("foo", instance, type)
                .execute();

            assertThat(h.createQuery("select bar from foo").mapTo(String.class).one())
                .isEqualTo("foo");

            return h.createQuery("select bar from foo")
                .mapTo(type)
                .one();
        });
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    public @interface Flavored {}

    private static class FixedJsonMapper implements JsonMapper {
        private final Object instance;
        private final String json;

        FixedJsonMapper(Object instance, String json) {
            this.instance = instance;
            this.json = json;
        }

        @Override
        public TypedJsonMapper forType(Type type, ConfigRegistry config) {
            return new TypedJsonMapper() {
                @Override
                public String toJson(Object value, ConfigRegistry config) {
                    assertThat(value).isEqualTo(instance);
                    return json;
                }

                @Override
                public Object fromJson(String readJson, ConfigRegistry config) {
                    assertThat(readJson).isEqualTo(json);
                    return instance;
                }
            };
        }
    }

    private static class QualifierRecordingJsonMapper extends FixedJsonMapper {
        private final List<QualifiedType<?>> boundTypes = new ArrayList<>();
        private final List<QualifiedType<?>> mappedTypes = new ArrayList<>();

        QualifierRecordingJsonMapper(Object instance, String json) {
            super(instance, json);
        }

        @Override
        public TypedJsonMapper forType(QualifiedType<?> type, ConfigRegistry config) {
            TypedJsonMapper delegate = super.forType(type.getType(), config);
            return new TypedJsonMapper() {
                @Override
                public String toJson(Object value, ConfigRegistry config) {
                    boundTypes.add(type);
                    return delegate.toJson(value, config);
                }

                @Override
                public Object fromJson(String json, ConfigRegistry config) {
                    mappedTypes.add(type);
                    return delegate.fromJson(json, config);
                }
            };
        }
    }

    public static class Foo {

        @Override
        public String toString() {
            return "I am Foot.";
        }
    }
}
