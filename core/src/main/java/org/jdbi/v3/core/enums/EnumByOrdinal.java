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
package org.jdbi.v3.core.enums;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.core.qualifier.Qualifier;

/**
 * Specifies that an {@link Enum} value should be bound or mapped as its {@link Enum#ordinal()}.
 *
 * This annotation can be placed on the source enum (class),
 * in {@link org.jdbi.v3.core.statement.SqlStatement#bindByType(String, Object, org.jdbi.v3.core.qualifier.QualifiedType)} calls,
 * or on SqlObject methods and parameters.
 *
 * The order of priority, highest to lowest, is
 * <ul>
 *     <li>binding/mapping point (fluent API or SqlObject)</li>
 *     <li>class annotation</li>
 *     <li>{@link Enums} config</li>
 * </ul>
 *
 * @see Enums
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@Qualifier
public @interface EnumByOrdinal {}
