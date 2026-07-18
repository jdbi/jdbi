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
package org.jdbi.guice;

import org.jdbi.core.Jdbi;

/**
 * Allows specific customizations of the {@link Jdbi} instance during creation, contributed through its
 * {@link Jdbi.Builder} while the instance is being assembled.
 */
@FunctionalInterface
public interface GuiceJdbiCustomizer {

    /** Does not modify the Jdbi instance. */
    GuiceJdbiCustomizer NOP = builder -> {};

    /**
     * Customize the {@link Jdbi} instance being assembled by contributing to its builder.
     *
     * @param builder the {@link Jdbi.Builder} for the instance being assembled.
     */
    void customize(Jdbi.Builder builder);
}
