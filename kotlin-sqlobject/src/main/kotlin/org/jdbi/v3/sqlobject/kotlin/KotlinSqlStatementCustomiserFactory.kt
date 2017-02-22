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

import org.jdbi.v3.core.generic.GenericTypes.findGenericParameter
import org.jdbi.v3.core.generic.GenericTypes.getErasedType
import org.jdbi.v3.core.kotlin.isKotlinClass
import org.jdbi.v3.core.statement.PreparedBatch
import org.jdbi.v3.core.statement.SqlStatement
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer
import org.jdbi.v3.sqlobject.statement.ParameterCustomizerFactory
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.declaredMemberProperties
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.kotlinFunction

class KotlinSqlStatementCustomiserFactory : ParameterCustomizerFactory {


    override fun createForParameter(sqlObjectType: Class<*>, method: Method, parameter: Parameter, paramIdx: Int): SqlStatementParameterCustomizer {
        val paramType = parameter.getParameterizedType()

        val bindName = if (parameter.isNamePresent()) {
            parameter.getName()
        } else {
            method.kotlinFunction?.parameters?.dropWhile { it.kind != KParameter.Kind.VALUE }?.toList()?.get(paramIdx)?.name
        } ?: throw UnsupportedOperationException("A parameter was not given a name, "
                + "and parameter name data is not present in the class file, for: "
                + parameter.getDeclaringExecutable() + " :: " + parameter)


        fun bind(q: SqlStatement<*>, bindToParm: String?, bindAsType: Type, value: Any?, prefix: String = "") {
            val type = if (q is PreparedBatch) {
                // FIXME BatchHandler should extract the iterable/iterator element type and pass it to the binder
                val erasedType = getErasedType(bindAsType)
                if (Iterable::class.java.isAssignableFrom(erasedType)) {
                    findGenericParameter(bindAsType, Iterable::class.java).get()
                } else if (Iterator::class.java.isAssignableFrom(erasedType)) {
                    findGenericParameter(bindAsType, Iterator::class.java).get()
                } else {
                    bindAsType
                }
            } else {
                bindAsType
            }

            val erasedType = getErasedType(type)
            if (erasedType.isKotlinClass()) {
                @Suppress("UNCHECKED_CAST")
                (erasedType.kotlin as KClass<Any>).declaredMemberProperties.forEach { subProp ->
                    bind(q, subProp.name, subProp.returnType.javaType, if (value == null) null else subProp.get(value), "${prefix}${bindToParm}.")
                }
            } else {
                if (bindToParm != null) {
                    q.bindByType("${prefix}${bindToParm}", value, type)
                } else if (prefix.isNullOrBlank()) {
                    // we can't really bind sub items by order
                    q.bindByType(paramIdx, value, type)
                }
            }
        }
        return SqlStatementParameterCustomizer { stmt, arg ->
            bind(stmt, bindName, paramType, arg)
        }
    }
}
