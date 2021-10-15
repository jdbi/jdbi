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
package org.jdbi.v3.sqlobject.kotlin

import org.jdbi.v3.sqlobject.config.ConfiguringAnnotation
import kotlin.reflect.KClass

/**
 * Registers a KotlinMapper for a specific kotlin class
 */
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@ConfiguringAnnotation(RegisterKotlinMapperImpl::class)
@kotlin.annotation.Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Repeatable
annotation class RegisterKotlinMapper(
    /**
     * The mapped kotlin class.
     * @return the mapped kotlin class.
     */
    val value: KClass<*>,
    /**
     * Column name prefix for the kotlin type. If omitted, defaults to no prefix.
     *
     * @return Column name prefix for the kotlin type.
     */
    val prefix: String = "")
