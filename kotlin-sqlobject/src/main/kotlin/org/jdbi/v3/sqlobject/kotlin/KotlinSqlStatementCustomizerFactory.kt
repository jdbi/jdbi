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

import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer
import org.jdbi.v3.sqlobject.statement.ParameterCustomizerFactory
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.lang.reflect.Type
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.kotlinFunction

class KotlinSqlStatementCustomizerFactory : ParameterCustomizerFactory {

    override fun createForParameter(sqlObjectType: Class<*>,
                                    method: Method,
                                    parameter: Parameter,
                                    paramIdx: Int,
                                    type: Type): SqlStatementParameterCustomizer<Any> {

        val bindName = if (parameter.isNamePresent) {
            parameter.name
        } else {
            method.kotlinFunction
                    ?.parameters
                    ?.dropWhile { it.kind != KParameter.Kind.VALUE }
                    ?.toList()
                    ?.get(paramIdx)?.name
        } ?: throw UnsupportedOperationException(
                "A parameter was not given a name, and parameter name data is not present in the class file, for: " +
                "${parameter.declaringExecutable} :: $parameter")

        return SqlStatementParameterCustomizer { stmt, arg ->
            val maybeArgument = stmt.getContext().findArgumentFor(type, arg)
            if (!maybeArgument.isPresent) {
                stmt.bindBean(bindName, arg)
            } else {
                val argument = maybeArgument.get()
                stmt.bind(bindName, argument)
                stmt.bind(paramIdx, argument)
            }
        }
    }
}
