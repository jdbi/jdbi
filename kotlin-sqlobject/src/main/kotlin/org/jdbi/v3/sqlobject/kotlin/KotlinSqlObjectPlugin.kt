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

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.extension.Extensions
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.kotlin.configure
import org.jdbi.v3.core.spi.JdbiPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.SqlObjects

/**
 * Installs Kotlin and SQLObject specific functionality.
 *
 *   @property installKotlinMapperFactory If true, install support for data and value classes. The default is `true`.
 *   @property enableCoroutineSupport If true, enable support for Kotlin Coroutines. The default is `false`.
 */
class KotlinSqlObjectPlugin(private val installKotlinMapperFactory: Boolean = true, private val enableCoroutineSupport: Boolean = false) :
    JdbiPlugin.Singleton() {
    override fun customizeJdbi(jdbi: Jdbi) {
        jdbi.installPlugin(KotlinPlugin(installKotlinMapperFactory = installKotlinMapperFactory, enableCoroutineSupport = enableCoroutineSupport))
        jdbi.installPlugin(SqlObjectPlugin())
        jdbi.configure(SqlObjects::class) { c -> c.defaultParameterCustomizerFactory = KotlinSqlStatementCustomizerFactory() }
        jdbi.configure(Extensions::class) { c -> c.registerHandlerFactory(KotlinDefaultMethodHandlerFactory()) }
    }
}
