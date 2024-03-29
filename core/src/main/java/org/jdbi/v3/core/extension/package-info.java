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
/**
 * <p>
 * The <code>extension</code> classes allow you to extend <code>Jdbi</code>'s
 * functionality by declaring interface types that may attach to <code>Handle</code>
 * instances.  An attached extension creates an instance of the interface that
 * wraps method invocations in an extension context.  The context has a
 * configuration and remembers the currently executing extension method.
 * An ExtensionFactory instance provides the actual extension instances and
 * implements the behavior of the extension.
 * Some built in features such as SQL Objects are themselves implemented as extensions.
 * </p>
 */
package org.jdbi.v3.core.extension;
