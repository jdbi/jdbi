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

import java.sql.Statement;

/**
 * Provides access to a result bearer and mod counts for batch statements.
 */
public interface ResultBearingAndModCounts {
    /**
     * Returns the {@link ResultBearing} for the executed {@link org.jdbi.v3.core.statement.PreparedBatch}
     *
     * @return the {@link ResultBearing} for the executed {@link org.jdbi.v3.core.statement.PreparedBatch}
     */
    ResultBearing resultBearing();

    /**
     * Returns the mod counts for the executed {@link org.jdbi.v3.core.statement.PreparedBatch}
     * Note that some database drivers might return special values like {@link Statement#SUCCESS_NO_INFO}
     * or {@link Statement#EXECUTE_FAILED}.
     * Attention! The result is only available after executing the statement (eg. calling map())!
     *
     * @return the number of rows affected per batch part for the executed {@link org.jdbi.v3.core.statement.PreparedBatch}
     */
    int[] modCounts();
}
