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

class RowMappersTest {

    private static final RowMapper<?> LAMBDA_MAPPER = (rs, ctx) -> "value";

    @Test
    void registerDoesNotMutateReceiver() {
        RowMappers base = new RowMappers();
        int baseSize = base.getFactories().size();

        RowMappers derived = base.register(RowMapperFactory.of(String.class, LAMBDA_MAPPER));

        assertThat(base.getFactories()).hasSize(baseSize);
        assertThat(derived.getFactories()).hasSize(baseSize + 1);
        assertThat(derived).isNotSameAs(base);
    }

    @Test
    void registerEmptyCollectionReturnsSameInstance() {
        RowMappers base = new RowMappers();
        assertThat(base.register(List.<RowMapperFactory>of())).isSameAs(base);
    }

    @Test
    void bulkRegisterLastFactoryWins() {
        RowMappers base = new RowMappers();
        RowMapperFactory first = RowMapperFactory.of(String.class, LAMBDA_MAPPER);
        RowMapperFactory last = RowMapperFactory.of(Integer.class, LAMBDA_MAPPER);

        List<RowMapperFactory> factories = base.register(List.of(first, last)).getFactories();

        // As with successive register() calls, the last factory in the collection has the highest priority (front).
        assertThat(factories.indexOf(last)).isLessThan(factories.indexOf(first));
    }

    @Test
    void withInferenceInterceptorIsConsultedWithoutMutatingReceiver() {
        RowMappers base = new RowMappers();
        // A non-concretely-parameterized mapper is rejected by the default inference.
        assertThatThrownBy(() -> base.register(LAMBDA_MAPPER)).isInstanceOf(UnsupportedOperationException.class);

        JdbiInterceptor<RowMapper<?>, RowMapperFactory> interceptor =
                (source, chain) -> RowMapperFactory.of(String.class, source);
        RowMappers derived = base.withInferenceInterceptor(interceptor);

        // The receiver is unchanged: it still rejects the mapper.
        assertThatThrownBy(() -> base.register(LAMBDA_MAPPER)).isInstanceOf(UnsupportedOperationException.class);

        // The derived config consults the interceptor, so registration succeeds.
        RowMappers registered = derived.register(LAMBDA_MAPPER);
        assertThat(registered.getFactories()).hasSize(derived.getFactories().size() + 1);
    }
}
