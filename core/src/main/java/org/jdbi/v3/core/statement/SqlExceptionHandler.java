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

import java.sql.SQLException;

import jakarta.annotation.Nullable;

import org.jdbi.v3.meta.Beta;

/**
 * Handler for exceptions thrown while executing SQL statements.
 */
@Beta
@FunctionalInterface
public interface SqlExceptionHandler {
    /**
     * Take action based on a SQLException thrown during statement execution.
     * If you return a replacement exception, before throwing it Jdbi will automatically
     * attach the original exception as "suppressed", so don't do that.
     * @param ex the exception thrown
     * @return a replacement exception to throw, or null to defer
     */
    @Nullable
    Throwable handle(SQLException ex);
}
