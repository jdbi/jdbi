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
 * Marker for a {@link SqlStatementCustomizer}, {@link SqlStatementParameterCustomizer}, or
 * {@link SqlStatementCustomizerFactory} whose customization mutates the statement configuration
 * (for example {@code getConfig(SqlStatements.class).setUnusedBindingAllowed(...)}) based on the
 * specific invocation, so that it cannot be applied once when a reusable template is built.
 *
 * <p>A SQL Object method carrying any such customizer is executed on the classic per-statement path,
 * where each invocation owns a private configuration copy. Methods without any config-mutating
 * customizer run on the faster shared-configuration template path.
 *
 * <p>A customizer that mutates configuration but does not declare itself with this marker will
 * silently mutate the shared template configuration, affecting other executions. Declare it here.
 */
public interface ConfigMutating {}
