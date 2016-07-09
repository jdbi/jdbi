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
package org.jdbi.v3.sqlobject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

/**
 * Causes the annotated method to be run in a transaction. Nested <code>@Transaction</code> annotations (e.g. one method
 * calls another method, where both methods have this annotation) are collapsed into a single transaction. The
 * transaction isolation level is the level specified out the outermost <code>@Transaction</code> annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@SqlMethodDecoratingAnnotation(Transaction.Decorator.class)
public @interface Transaction
{
    TransactionIsolationLevel value() default TransactionIsolationLevel.INVALID_LEVEL;

    class Decorator implements HandlerDecorator {
        @Override
        public Handler decorateHandler(Handler handler, Class<?> sqlObjectType, Method method) {
            return new TransactionDecorator(handler, method.getAnnotation(Transaction.class));
        }
    }
}
