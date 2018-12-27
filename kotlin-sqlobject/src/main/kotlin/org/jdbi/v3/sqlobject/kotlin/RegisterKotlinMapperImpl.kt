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

import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.kotlin.KotlinMapper
import org.jdbi.v3.core.mapper.RowMappers
import org.jdbi.v3.sqlobject.config.Configurer
import java.lang.reflect.Method

class RegisterKotlinMapperImpl : Configurer {

    override fun configureForType(registry: ConfigRegistry, annotation: Annotation, sqlObjectType: Class<*>) {
        val registerKotlinMapper = annotation as RegisterKotlinMapper
        val kotlinClass = registerKotlinMapper.value
        val prefix = registerKotlinMapper.prefix
        val mappers = registry.get(RowMappers::class.java)
        if (prefix.isEmpty()) {
            mappers.register(kotlinClass.java, KotlinMapper(kotlinClass.java))
        } else {
            mappers.register(kotlinClass.java, KotlinMapper(kotlinClass.java, prefix))
        }
    }

    override fun configureForMethod(registry: ConfigRegistry, annotation: Annotation, sqlObjectType: Class<*>, method: Method) {
        configureForType(registry, annotation, sqlObjectType)
    }
}
