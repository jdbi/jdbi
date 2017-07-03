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
 * Wraps exceptions thrown while producing Java result types.
 */
public class UnableToProduceResultException extends StatementException
{
    private static final long serialVersionUID = 1L;

    public UnableToProduceResultException(Exception e, StatementContext ctx) {
        super(e, ctx);
    }

    public UnableToProduceResultException(String message, StatementContext ctx)
    {
        super(message, ctx);
    }

    public UnableToProduceResultException(String string, Throwable throwable, StatementContext ctx)
    {
        super(string, throwable, ctx);
    }
}
