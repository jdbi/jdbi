/**
 * Copyright (c), Data Geekery GmbH, contact@datageekery.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * A specification for a {@link Window}.
 *
 * @author Lukas Eder
 */
public interface WindowSpecification<T> {

    /**
     * The window partition function.
     */
    Function<? super T, ?> partition();

    /**
     * The window order.
     */
    Optional<Comparator<? super T>> order();

    /**
     * The window's lower frame bound.
     */
    long lower();

    /**
     * The window's upper frame bound.
     */
    long upper();
}
