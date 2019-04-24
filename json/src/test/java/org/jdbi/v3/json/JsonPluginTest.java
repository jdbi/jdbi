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

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JsonPluginTest {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new JsonPlugin());
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();
    @Mock
    public JsonMapper jsonMapper;

    @Before
    public void before() {
        db.getJdbi().getConfig(JsonConfig.class).setJsonMapper(jsonMapper);
        db.getJdbi().useHandle(h -> h.createUpdate("create table foo(bar varchar)").execute());
    }

    @Test
    public void factoryChainWorks() {
        Object instance = new Foo();
        String json = "foo";

        when(jsonMapper.toJson(eq(Foo.class), eq(instance), any(ConfigRegistry.class))).thenReturn(json);
        when(jsonMapper.fromJson(eq(Foo.class), eq(json), any(ConfigRegistry.class))).thenReturn(instance);

        Object result = db.getJdbi().withHandle(h -> {
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
        verify(jsonMapper).fromJson(eq(Foo.class), eq(json), any(ConfigRegistry.class));
        verify(jsonMapper).toJson(eq(Foo.class), eq(instance), any(ConfigRegistry.class));
    }

    public static class Foo {
        @Override
        public String toString() {
            return "I am Foot.";
        }
    }
}
