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

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import kotlin.reflect.KClass

inline fun <reified T : Any> Jdbi.onDemand(): T {
    return this.onDemand(T::class.java)
}

fun <T : Any> Jdbi.onDemand(kclass: KClass<T>): T {
    return this.onDemand(kclass.java)
}

inline fun <reified T : Any> Handle.attach(): T {
    return this.attach(T::class.java)
}

fun <T : Any> Handle.attach(kclass: KClass<T>): T {
    return this.attach(kclass.java)
}
