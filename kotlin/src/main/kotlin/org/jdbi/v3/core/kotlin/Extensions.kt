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
package org.jdbi.v3.core.kotlin


import org.jdbi.v3.core.result.ResultBearing
import org.jdbi.v3.core.result.ResultIterable

private val metadataFqName = "kotlin.Metadata"

fun Class<*>.isKotlinClass(): Boolean {
    return this.annotations.singleOrNull { it.annotationClass.java.name == metadataFqName } != null
}

inline fun <reified T : Any> ResultBearing.mapTo(): ResultIterable<T> {
    return this.map(KotlinMapper(T::class.java))
}

inline fun <O : Any> ResultIterable<O>.useSequence(block: (Sequence<O>) -> Unit): Unit {
    this.iterator().use {
        block(it.asSequence())
    }
}

