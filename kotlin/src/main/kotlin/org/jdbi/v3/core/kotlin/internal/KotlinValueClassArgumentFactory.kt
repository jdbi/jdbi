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
package org.jdbi.v3.core.kotlin.internal

import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.argument.ArgumentFactory
import org.jdbi.v3.core.argument.Arguments
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.meta.Alpha
import java.lang.reflect.Type
import java.util.Optional
import kotlin.reflect.full.memberProperties

/**
 * An [ArgumentFactory] which supports Kotlin value classes.
 */
@Alpha
class KotlinValueClassArgumentFactory : ArgumentFactory {
    override fun build(type: Type, value: Any?, config: ConfigRegistry): Optional<Argument> {
        val clazz = (type as? Class<*>)?.kotlin?.also {
            if (!it.isValue) return Optional.empty()
        } ?: return Optional.empty()

        val property = clazz.memberProperties.singleOrNull()
        if (property == null) return Optional.empty()

        val valueParameterJavaType = toJavaType(property.returnType)
        val value = property.call(value)

        return config[Arguments::class.java].findFor(valueParameterJavaType, value)
    }

    override fun toString(): String = "KotlinValueClassArgumentFactory"
}
