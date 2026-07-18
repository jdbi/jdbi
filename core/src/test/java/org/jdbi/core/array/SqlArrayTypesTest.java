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
package org.jdbi.core.array;

import java.math.BigInteger;

import org.jdbi.core.interceptor.JdbiInterceptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlArrayTypesTest {

    @Test
    void registerDoesNotMutateReceiver() {
        SqlArrayTypes base = new SqlArrayTypes();
        int baseSize = base.getFactories().size();

        SqlArrayTypes derived = base.register(BigInteger.class, "numeric");

        assertThat(base.getFactories()).hasSize(baseSize);
        assertThat(derived.getFactories()).hasSize(baseSize + 1);
        assertThat(derived).isNotSameAs(base);
    }

    @Test
    void argumentStrategyWitherDoesNotMutateReceiver() {
        SqlArrayTypes base = new SqlArrayTypes();
        assertThat(base.getArgumentStrategy()).isEqualTo(SqlArrayArgumentStrategy.SQL_ARRAY);

        SqlArrayTypes derived = base.argumentStrategy(SqlArrayArgumentStrategy.OBJECT_ARRAY);

        assertThat(base.getArgumentStrategy()).isEqualTo(SqlArrayArgumentStrategy.SQL_ARRAY);
        assertThat(derived.getArgumentStrategy()).isEqualTo(SqlArrayArgumentStrategy.OBJECT_ARRAY);
        assertThat(derived).isNotSameAs(base);
    }

    @Test
    void withInferenceInterceptorReturnsNewInstanceWithoutMutatingReceiver() {
        SqlArrayTypes base = new SqlArrayTypes();
        int baseSize = base.getFactories().size();

        JdbiInterceptor<SqlArrayType<?>, SqlArrayTypeFactory> interceptor =
                (source, chain) -> chain.next();
        SqlArrayTypes derived = base.withInferenceInterceptor(interceptor);

        assertThat(derived).isNotSameAs(base);
        assertThat(base.getFactories()).hasSize(baseSize);
    }
}
