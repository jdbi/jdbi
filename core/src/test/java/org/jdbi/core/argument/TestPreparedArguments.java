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
package org.jdbi.core.argument;

import org.jdbi.core.Handle;
import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPreparedArguments {
    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @Test
    public void disablePreparedArguments() {
        final Handle h = h2Extension.getSharedHandle();

        assertThat(ArgumentResolver.forRegistry(h.getConfig()).prepareFor(int.class))
                .isNotEmpty();

        // A config scope on a freshly opened handle disables prepared arguments; the resolver is scoped to that
        // handle's private registry, so it resolves against the disabled configuration.
        try (Handle disabled = h2Extension.getJdbi().open(cfg -> cfg.configure(Arguments.class, c -> c.preparedArgumentsEnabled(false)))) {
            assertThat(ArgumentResolver.forRegistry(disabled.getConfig()).prepareFor(int.class))
                    .isEmpty();
        }
    }
}
