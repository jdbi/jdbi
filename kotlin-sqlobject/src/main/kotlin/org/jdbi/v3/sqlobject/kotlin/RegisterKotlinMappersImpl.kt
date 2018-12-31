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
import org.jdbi.v3.sqlobject.config.Configurer
import org.jdbi.v3.sqlobject.config.RegisterKotlinMappers
import java.lang.reflect.Method

class RegisterKotlinMappersImpl : Configurer {

    override fun configureForType(registry: ConfigRegistry, annotation: Annotation, sqlObjectType: Class<*>) {
        val delegate = RegisterKotlinMapperImpl()

        val registerKotlinMappers = annotation as RegisterKotlinMappers
        registerKotlinMappers.value.forEach { anno -> delegate.configureForType(registry, anno, sqlObjectType) }
    }

    override fun configureForMethod(registry: ConfigRegistry, annotation: Annotation, sqlObjectType: Class<*>, method: Method) {
        configureForType(registry, annotation, sqlObjectType)
    }
}
