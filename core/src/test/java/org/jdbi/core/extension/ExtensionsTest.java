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
package org.jdbi.core.extension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionsTest {

    @Test
    void registerHandlerCustomizerDoesNotMutateReceiver() {
        Extensions base = new Extensions();
        int baseSize = base.getExtensionHandlerCustomizers().size();

        Extensions derived = base.registerHandlerCustomizer((handler, sqlObjectType, method) -> handler);

        assertThat(base.getExtensionHandlerCustomizers()).hasSize(baseSize);
        assertThat(derived.getExtensionHandlerCustomizers()).hasSize(baseSize + 1);
        assertThat(derived).isNotSameAs(base);
    }

    @Test
    void allowProxyWitherDoesNotMutateReceiver() {
        Extensions base = new Extensions();
        assertThat(base.isAllowProxy()).isTrue();

        Extensions derived = base.allowProxy(false);

        assertThat(base.isAllowProxy()).isTrue();
        assertThat(derived.isAllowProxy()).isFalse();
        assertThat(derived).isNotSameAs(base);
    }

    @Test
    void failFastWitherDoesNotMutateReceiver() {
        Extensions base = new Extensions();
        assertThat(base.isFailFast()).isFalse();

        Extensions derived = base.failFast();

        assertThat(base.isFailFast()).isFalse();
        assertThat(derived.isFailFast()).isTrue();
        assertThat(derived).isNotSameAs(base);
    }
}
