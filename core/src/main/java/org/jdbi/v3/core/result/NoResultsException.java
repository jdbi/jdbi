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
package org.jdbi.v3.core.result;

import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementException;

/**
 * Thrown when no results were found in a context that requires at least one.
 */
public class NoResultsException extends StatementException {
    private static final long serialVersionUID = 1L;

    public NoResultsException(String msg, Throwable e, StatementContext ctx) {
        super(msg, e, ctx);
    }

    public NoResultsException(Throwable e, StatementContext ctx) {
        super(e, ctx);
    }

    public NoResultsException(String msg, StatementContext ctx) {
        super(msg, ctx);
    }
}
