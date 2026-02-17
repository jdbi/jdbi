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
package org.jdbi.json;

import java.lang.reflect.Type;

import org.jdbi.core.Jdbi;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonPluginTest {
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

    public static class Foo {

        @Override
        public String toString() {
            return "I am Foot.";
        }
    }
}
