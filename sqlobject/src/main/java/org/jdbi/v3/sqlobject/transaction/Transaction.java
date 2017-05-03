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
package org.jdbi.v3.sqlobject.transaction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.SqlMethodDecoratingAnnotation;
import org.jdbi.v3.sqlobject.transaction.internal.TransactionDecorator;

/**
 * Causes the annotated method to be run in a transaction.
 * <p>
 * Nested <code>@Transaction</code> annotations (e.g. one method calls another method, where both methods have this
 * annotation) are collapsed into a single transaction. If the outer method annotation specifies an isolation level,
 * then the inner method must either specify the same level, or not specify any level.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@SqlMethodDecoratingAnnotation(TransactionDecorator.class)
public @interface Transaction {
    /**
     * @return the transaction isolation level.  If not specified, invoke with the default isolation level.
     */
    TransactionIsolationLevel value() default TransactionIsolationLevel.UNKNOWN;
    /**
     * Set the connection readOnly property before the transaction starts, and restore it before it returns.
     * Databases may use this as a performance or concurrency hint.
     * @return whether the transaction is read only
     */
    boolean readOnly() default false;
}
