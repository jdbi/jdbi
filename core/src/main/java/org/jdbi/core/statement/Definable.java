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
package org.jdbi.core.statement;

/**
 * A target on which defined attributes ("defines") can be set for use by a {@link TemplateEngine}.
 * Implemented by SQL statements and by query template bindings, so that empty-list handlers and
 * other define-based helpers can operate uniformly regardless of the concrete statement type.
 *
 * @param <This> the fluent self type returned by {@link #define}
 */
public interface Definable<This> {

    /**
     * Defines an attribute for use by the template engine.
     *
     * @param key   the attribute name
     * @param value the attribute value
     * @return this
     */
    This define(String key, Object value);
}
