/**
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
package org.jdbi.v3.sqlobject.kotlin

import org.jdbi.v3.meta.Beta
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation
import org.jdbi.v3.sqlobject.kotlin.internal.BindKotlinFactory

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
@SqlStatementCustomizingAnnotation(BindKotlinFactory::class)
@Beta
annotation class BindKotlin(
    /**
     * Prefix to apply to each property. If specified, properties will be bound as
     * `prefix.propertyName`.
     *
     * @return the prefix
     */
    val value:String = ""
)
