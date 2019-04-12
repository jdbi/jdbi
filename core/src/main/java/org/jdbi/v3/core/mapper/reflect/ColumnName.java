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
package org.jdbi.v3.core.mapper.reflect;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specify the mapping name for a property or parameter explicitly. This annotation is respected by:
 *
 * <ul>
 *     <li>BeanMapper, FieldMapper, and ConstructorMapper in core</li>
 *     <li>The Kotlin data class mapper in KotlinPlugin</li>
 * </ul>
 *
 * Note that this annotation only applies to mapping, not parameter binding. When binding with e.g.
 * {@link org.jdbi.v3.core.statement.SqlStatement#bindBean(Object) bindBean()}, bind parameters by
 * the property name ({@code :firstName}), not the column name ({@code :first_name}).
 */
@Retention(RUNTIME)
@Target({PARAMETER, FIELD, METHOD})
public @interface ColumnName {
    String value();
}
