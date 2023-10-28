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

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.RowMappers
import org.jdbi.v3.core.spi.JdbiPlugin

/**
 * Installs Kotlin specific functionality.
 *
 * <ul>
 *     <li>Kotlin inference interceptor that handles the {@link KotlinMapper} for registered types</li>
 *     <li>Kotlin mapper factory that creates implicit mappers for any data class</li>
 *     <li>Support for using Handles in Coroutines</li>
 * <ul>
 *
 *     @property installKotlinMapperFactory If true, install the {@link KotlinMapperFactory}.
 *     @property enableCoroutineSupport If true, enable support for Kotlin Coroutines.
 */
class KotlinPlugin(
    private val installKotlinMapperFactory: Boolean = true,
    private val enableCoroutineSupport: Boolean = false
) : JdbiPlugin.Singleton() {

    override fun customizeJdbi(jdbi: Jdbi) {
        jdbi.configure(RowMappers::class.java) {
            it.inferenceInterceptors.addFirst(KotlinRowMapperInterceptor())
        }

        if (installKotlinMapperFactory) {
            jdbi.registerRowMapper(KotlinMapperFactory())
        }

        // install a special handle scope that can deal with coroutines.
        //
        // Note that this needs to be revisited in the future if we move from the ThreadLocal in Jdbi
        // to e.g. structured concurrency. Right now, this delegates to a ThreadLocalHandleScope.
        if (enableCoroutineSupport) {
            jdbi.handleScope = CoroutineHandleScope()
        }
    }
}
