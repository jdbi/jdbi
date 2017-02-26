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

import org.jdbi.v3.core.generic.GenericTypes
import org.jdbi.v3.core.kotlin.isKotlinClass
import org.jdbi.v3.core.statement.PreparedBatch
import org.jdbi.v3.core.statement.SqlStatement
import org.jdbi.v3.sqlobject.SingleValue
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

        val bindName = if (parameter.isNamePresent) {
            parameter.name
        } else {
            method.kotlinFunction?.parameters?.dropWhile { it.kind != KParameter.Kind.VALUE }?.toList()?.get(paramIdx)?.name
        } ?: throw UnsupportedOperationException("A parameter was not given a name, and parameter name data is not present in the class file, for: " +
                "${parameter.declaringExecutable} :: $parameter")


        fun bind(q: SqlStatement<*>, bindToParm: String, type: Type, value: Any?, prefix: String = "") {

            val erasedType = GenericTypes.getErasedType(type)
            if (erasedType.isKotlinClass()) {
                @Suppress("UNCHECKED_CAST")
                (erasedType.kotlin as KClass<Any>).declaredMemberProperties.forEach { subProp ->
                    bind(q, subProp.name, subProp.returnType.javaType, if (value == null) null else subProp.get(value), "$prefix$bindToParm.")
                }
            } else {
                q.bindByType("$prefix$bindToParm", value, type)
                if (prefix.isBlank()) {
                    // we can't really bind sub items by order
                    q.bindByType(paramIdx, value, type)
                }
            }
        }
        return SqlStatementParameterCustomizer { stmt, arg ->
            val paramType = parameter.parameterizedType

            val type = if (stmt is PreparedBatch && !parameter.isAnnotationPresent(SingleValue::class.java)) {
                // FIXME BatchHandler should extract the iterable/iterator element type and pass it to the binder
                val erasedType = GenericTypes.getErasedType(paramType)
                if (Iterable::class.java.isAssignableFrom(erasedType)) {
                    GenericTypes.findGenericParameter(paramType, Iterable::class.java).get()
                } else if (Iterator::class.java.isAssignableFrom(erasedType)) {
                    GenericTypes.findGenericParameter(paramType, Iterator::class.java).get()
                } else if (GenericTypes.isArray(paramType)) {
                    (paramType as Class<*>).componentType
                } else {
                    paramType
                }
            } else {
                paramType
            }

            bind(stmt, bindName, type, arg)

        }
    }
}
