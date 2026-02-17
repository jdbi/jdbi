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
package org.jdbi.sqlobject.kotlin.internal

import org.jdbi.core.kotlin.internal.KotlinPropertyArguments
import org.jdbi.sqlobject.customizer.SqlStatementCustomizerFactory
import org.jdbi.sqlobject.customizer.SqlStatementParameterCustomizer
import org.jdbi.sqlobject.kotlin.BindKotlin
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.lang.reflect.Type

class BindKotlinFactory : SqlStatementCustomizerFactory {
    override fun createForParameter(
        annotation: Annotation,
        sqlObjectType: Class<*>,
        method: Method,
        param: Parameter,
        index: Int,
        paramType: Type
    ): SqlStatementParameterCustomizer {
        val bindKotlin = annotation as BindKotlin
        return SqlStatementParameterCustomizer { stmt, arg ->
            // would prefer stmt.bindKotlin() but type checker doesn't like that stmt is wildcard
            stmt.bindNamedArgumentFinder(KotlinPropertyArguments(arg, bindKotlin.value))
        }
    }
}
