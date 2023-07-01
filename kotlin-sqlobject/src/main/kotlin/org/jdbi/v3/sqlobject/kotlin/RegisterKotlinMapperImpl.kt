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

import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.extension.SimpleExtensionConfigurer
import org.jdbi.v3.core.kotlin.KotlinMapper
import org.jdbi.v3.core.mapper.RowMappers

class RegisterKotlinMapperImpl(annotation: Annotation) : SimpleExtensionConfigurer() {
    private val registerKotlinMapper = annotation as RegisterKotlinMapper
    private val kotlinClass = registerKotlinMapper.value
    private val prefix = registerKotlinMapper.prefix
    private val kotlinMapper = KotlinMapper(kotlinClass.java, prefix)

    override fun configure(config: ConfigRegistry, annotation: Annotation, sqlObjectType: Class<*>) {
        val mappers = config.get(RowMappers::class.java)
        mappers.register(kotlinClass.java, kotlinMapper)
    }
}
