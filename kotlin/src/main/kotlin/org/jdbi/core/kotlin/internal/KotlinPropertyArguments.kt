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

package org.jdbi.core.kotlin.internal

import org.jdbi.core.argument.NamedArgumentFinder
import org.jdbi.core.argument.internal.ObjectPropertyNamedArgumentFinder
import org.jdbi.core.argument.internal.TypedValue
import org.jdbi.core.kotlin.getQualifiers
import org.jdbi.core.qualifier.QualifiedType
import org.jdbi.core.statement.StatementContext
import java.util.Optional
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class KotlinPropertyArguments(obj: Any, prefix: String = "") : ObjectPropertyNamedArgumentFinder(prefix, obj) {

    private val kClass: KClass<*> = obj.javaClass.kotlin
    private val properties = kClass.memberProperties
        .associateBy { it.name }

    override fun getNames(): Collection<String> = properties.keys

    override fun getValue(name: String, ctx: StatementContext): Optional<TypedValue> {
        val property: KProperty1<*, *> = properties[name] ?: return Optional.empty()
        val mutableProperty = property as? KMutableProperty1
        val type = QualifiedType.of(toJavaType(property.returnType))
            .withAnnotations(
                getQualifiers(
                    kClass.primaryConstructor?.run { parameters.find { it.name == name } },
                    property,
                    property.getter,
                    mutableProperty?.setter,
                    mutableProperty?.run { setter.parameters.getOrNull(0) }
                )
            )
        val value = property.getter.call(obj)
        return Optional.of(TypedValue(type, value))
    }

    override fun getNestedArgumentFinder(obj: TypedValue): NamedArgumentFinder = KotlinPropertyArguments(obj.value)

    override fun toString() = "KotlinPropertyArguments(obj=${kClass.qualifiedName}, prefix=$prefix)"
}
