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
package org.jdbi.v3.core;

import java.sql.SQLException;

/**
 * Cleans up some JDBC resource e.g. after completion execution of a SQL statement. Arguments, mappers, and other
 * JDBI interface implementations that allocate database resources should register a Cleanable to ensure that
 * resources are freed after database operations are completed.
 *
 * @see BaseStatement#addCleanable(Cleanable)
 * @see StatementContext#addCleanable(Cleanable)
 */
@FunctionalInterface
public interface Cleanable extends AutoCloseable
{
    @Override
    void close() throws SQLException;
}
