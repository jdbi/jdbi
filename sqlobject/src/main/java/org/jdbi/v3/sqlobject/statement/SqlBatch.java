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
package org.jdbi.v3.sqlobject.statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.sqlobject.SqlOperation;
import org.jdbi.v3.sqlobject.statement.internal.SqlBatchHandler;

/**
 * Annotate a method to indicate that it will create and execute a SQL batch. At least one
 * bound argument must be an Iterator or Iterable, values from this will be taken and applied
 * to each row of the batch. Non iterable bound arguments will be treated as constant values and
 * bound to each row.
 * <p>
 * Unfortunately, because of how batches work, statement customizers and sql statement customizers
 * which affect SQL generation will *not* work with batches. This primarily effects statement location
 * and rewriting, which will always use the values defined on the bound Handle.
 * <p>
 * If you want to chunk up the logical batch into a number of smaller batches (say around 1000 rows at
 * a time in order to not wreck havoc on the transaction log, you should see
 * {@link BatchChunkSize}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@SqlOperation(SqlBatchHandler.class)
public @interface SqlBatch {
    /**
     * @return the SQL string (or name)
     */
    String value() default "";

    /**
     * @return whether to execute the batch chunks in a transaction. Default is true (and it will be strange if you
     * want otherwise).
     */
    boolean transactional() default true;
}
