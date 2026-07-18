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
package org.jdbi.core.mapper;

import java.util.List;

import org.jdbi.core.interceptor.JdbiInterceptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ColumnMappersTest {

    private static final ColumnMapper<?> LAMBDA_MAPPER = (rs, columnNumber, ctx) -> "value";

    @Test
    void registerDoesNotMutateReceiver() {
        ColumnMappers base = new ColumnMappers();
        int baseSize = base.getFactories().size();

        ColumnMappers derived = base.register(ColumnMapperFactory.of(String.class, LAMBDA_MAPPER));

        assertThat(base.getFactories()).hasSize(baseSize);
        assertThat(derived.getFactories()).hasSize(baseSize + 1);
        assertThat(derived).isNotSameAs(base);
    }

    @Test
    void registerEmptyCollectionReturnsSameInstance() {
        ColumnMappers base = new ColumnMappers();
        assertThat(base.register(List.<ColumnMapperFactory>of())).isSameAs(base);
    }

    @Test
    void coalesceNullPrimitivesWitherDoesNotMutateReceiver() {
        ColumnMappers base = new ColumnMappers();
        assertThat(base.getCoalesceNullPrimitivesToDefaults()).isTrue();

        ColumnMappers derived = base.coalesceNullPrimitivesToDefaults(false);

        assertThat(base.getCoalesceNullPrimitivesToDefaults()).isTrue();
        assertThat(derived.getCoalesceNullPrimitivesToDefaults()).isFalse();
        assertThat(derived).isNotSameAs(base);
    }

    @Test
    void withInferenceInterceptorIsConsultedWithoutMutatingReceiver() {
        ColumnMappers base = new ColumnMappers();
        assertThatThrownBy(() -> base.register(LAMBDA_MAPPER)).isInstanceOf(UnsupportedOperationException.class);

        JdbiInterceptor<ColumnMapper<?>, QualifiedColumnMapperFactory> interceptor =
                (source, chain) -> QualifiedColumnMapperFactory.adapt(ColumnMapperFactory.of(String.class, source));
        ColumnMappers derived = base.withInferenceInterceptor(interceptor);

        assertThatThrownBy(() -> base.register(LAMBDA_MAPPER)).isInstanceOf(UnsupportedOperationException.class);

        ColumnMappers registered = derived.register(LAMBDA_MAPPER);
        assertThat(registered.getFactories()).hasSize(derived.getFactories().size() + 1);
    }
}
