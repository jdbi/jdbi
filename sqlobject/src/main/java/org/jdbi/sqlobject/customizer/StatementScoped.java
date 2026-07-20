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
package org.jdbi.sqlobject.customizer;

/**
 * Marker for a {@link SqlStatementCustomizer} or {@link SqlStatementCustomizerFactory} that operates on
 * the executable statement instance rather than on configuration &mdash; for example, registering an out
 * parameter on the {@link org.jdbi.core.statement.Call} produced for the invocation. Such a customizer
 * cannot be applied to the build-time configuration surface used when a reusable template is built, so it
 * is applied to each invocation's live statement instead.
 *
 * <p>Type- and method-level customizers are otherwise treated as invariant configuration and baked once
 * into the shared template configuration. Declare this marker on a type- or method-level customizer that
 * must touch the live {@link org.jdbi.core.statement.Query}, {@link org.jdbi.core.statement.Call}, or
 * {@link org.jdbi.core.statement.PreparedBatch}; otherwise it will be handed a configuration surface that
 * does not support statement operations and the invocation will fail.
 *
 * <p>This is distinct from {@link ConfigMutating}: a {@code ConfigMutating} customizer changes
 * configuration per invocation (and so forces the classic per-statement path), whereas a
 * {@code StatementScoped} customizer leaves configuration unchanged and only acts on the statement, so
 * the invocation still uses the shared-configuration template path.
 */
public interface StatementScoped {}
