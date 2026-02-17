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
package org.jdbi.sqlobject.kotlin

import org.jdbi.core.Handle
import org.jdbi.core.Jdbi
import kotlin.reflect.KClass

inline fun <reified T : Any> Jdbi.onDemand(): T = this.onDemand(T::class.java)

fun <T : Any> Jdbi.onDemand(kclass: KClass<T>): T = this.onDemand(kclass.java)

inline fun <reified T : Any> Handle.attach(): T = this.attach(T::class.java)

fun <T : Any> Handle.attach(kclass: KClass<T>): T = this.attach(kclass.java)
