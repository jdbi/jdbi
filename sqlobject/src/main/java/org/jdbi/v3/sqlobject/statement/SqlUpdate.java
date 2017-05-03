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

import org.jdbi.v3.sqlobject.SqlMethodAnnotation;
import org.jdbi.v3.sqlobject.statement.internal.SqlUpdateHandler;

/**
 * Used to indicate that a method should execute a non-query sql statement.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@SqlMethodAnnotation(SqlUpdateHandler.class)
public @interface SqlUpdate {
    /**
     * The sql (or statement name if using a statement locator) to be executed. The default value will use
     * the method name of the method being annotated. This default behavior is only useful in conjunction
     * with a statement locator.
     *
     * @return the SQL string (or name)
     */
    String value() default "";
}
