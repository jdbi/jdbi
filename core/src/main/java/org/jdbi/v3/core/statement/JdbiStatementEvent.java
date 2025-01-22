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

import jdk.jfr.Label;
import org.jdbi.v3.core.statement.internal.OptionalEvent;

@jdk.jfr.Category("jdbi")
@jdk.jfr.Name(JdbiStatementEvent.NAME)
@Label("Jdbi Statement")
public final class JdbiStatementEvent extends jdk.jfr.Event implements OptionalEvent {
    public static final String NAME = "jdbi.statement";
    @Label("Type")
    public String type;

    @Label("SQL")
    public String sql;

    @Label("Parameters")
    public String parameters;

    @Label("Result rows mapped")
    public long rowsMapped;

    @Label("Trace ID")
    public String traceId;
}
