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
package org.jdbi.v3.core.mapper.reflect;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.statement.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.jdbi.v3.core.junit5.H2DatabaseExtension.USERS_INITIALIZER;

public class ReflectionMappersAccessibleStrategyTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(USERS_INITIALIZER);

    Handle handle;

    @BeforeEach
    final void setUp() {
        this.handle = h2Extension.getSharedHandle();
    }

    @Test
    void testDefaults() {
        try (Query query = handle.createQuery("SELECT id as result FROM users order by id")) {
            List<?> fields = query.map(FieldMapper.of(PrivateConstructorBean.class)).list();
            assertThat(fields).hasSize(2);
            assertThat(fields).extracting("result").containsExactly(1L, 2L);

            fields = query.map(FieldMapper.of(PrivateFieldBean.class)).list();
            assertThat(fields).hasSize(2);
            assertThat(fields).extracting("result").containsExactly(1L, 2L);

            fields = query.map(FieldMapper.of(PublicFieldBean.class)).list();
            assertThat(fields).hasSize(2);
            assertThat(fields).extracting("result").containsExactly(1L, 2L);
        }
    }

    @Test
    void testAccessDisabledConstructorBean() {
        handle.getConfig(ReflectionMappers.class).disableAccessibleObjectStrategy();

        try (Query query = handle.createQuery("SELECT id as result FROM users order by id")) {
            assertThatThrownBy(() -> query.map(FieldMapper.of(PrivateConstructorBean.class)).list()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void testAccessDisabledFieldBean() {
        handle.getConfig(ReflectionMappers.class).disableAccessibleObjectStrategy();

        try (Query query = handle.createQuery("SELECT id as result FROM users order by id")) {
            assertThatThrownBy(() -> query.map(FieldMapper.of(PrivateFieldBean.class)).list()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void testAccessDisabledPublicBean() {
        handle.getConfig(ReflectionMappers.class).disableAccessibleObjectStrategy();

        try (Query query = handle.createQuery("SELECT id as result FROM users order by id")) {
            List<?> fields = query.map(FieldMapper.of(PublicFieldBean.class)).list();
            assertThat(fields).hasSize(2);
            assertThat(fields).extracting("result").containsExactly(1L, 2L);
        }
    }

    public static class PrivateConstructorBean {

        public long result;

        private PrivateConstructorBean() {}
    }

    public static class PrivateFieldBean {

        private long result;
    }

    public static class PublicFieldBean {
        public long result;
    }
}
