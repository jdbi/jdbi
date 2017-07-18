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
package org.jdbi.v3.core.statement;

/**
 * Renders an SQL statement from a template.
 *
 * @see DefinedAttributeTemplateEngine
 */
@FunctionalInterface
public interface TemplateEngine {
    /**
     * Renders an SQL statement from the given template, using the statement
     * context as needed.
     *
     * @param template The SQL to rewrite
     * @param ctx      The statement context for the statement being executed
     * @return something which can provide the actual SQL to prepare a statement from
     * and which can bind the correct arguments to that prepared statement
     */
    String render(String template, StatementContext ctx);
}
