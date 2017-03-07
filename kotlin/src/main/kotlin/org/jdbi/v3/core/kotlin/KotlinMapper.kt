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
package org.jdbi.v3.core.kotlin

import org.jdbi.v3.core.mapper.NoSuchMapperException
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.mapper.reflect.ColumnNameMatcher
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers
import org.jdbi.v3.core.statement.StatementContext
import java.lang.reflect.InvocationTargetException
import java.sql.ResultSet
import java.sql.SQLException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaType
import kotlin.reflect.full.primaryConstructor

class KotlinMapper<C : Any>(private val clazz: Class<C>) : RowMapper<C> {

    private val kClass: KClass<C> = clazz.kotlin

    @Throws(SQLException::class)
    override fun map(rs: ResultSet, ctx: StatementContext): C {
        return specialize(rs, ctx).map(rs, ctx)
    }

    @Throws(SQLException::class)
    override fun specialize(rs: ResultSet, ctx: StatementContext): RowMapper<C> {

        val constructor = findConstructor(kClass)
        // TODO: best fit for constructors + writeable properties, pay attention to nullables/optionals with default values
        //       for now just call primary constructor using named params and hope

        val validConstructorParameters = constructor.parameters.filter { it.kind == KParameter.Kind.VALUE && it.name != null }

        val metaData = rs.metaData
        val columnNameMatchers = ctx.getConfig(ReflectionMappers::class.java).columnNameMatchers
        val matchingConstructorParams = (metaData.columnCount downTo 1)
                .map { validConstructorParameters.matchColumnName(metaData.getColumnLabel(it), columnNameMatchers) }
                .filterNotNull()
                .toSet()


        // things missing from the result set that are Nullable and not optional should be set to Null
        val nullablesThatAreAbsent = constructor.parameters.filter { !it.isOptional && it.type.isMarkedNullable && it !in matchingConstructorParams }

        val matchingParamsWithColumnMappers = matchingConstructorParams.map { param ->
            val paramType = param.type.javaType
            val columnMapper = ctx.findColumnMapperFor(paramType).orElseThrow { NoSuchMapperException("No column mapper for " + paramType) }
            Pair(param, columnMapper)
        }

        val paramsWithColumnMappers = matchingParamsWithColumnMappers + nullablesThatAreAbsent.map { Pair(it, null) }

        return RowMapper { r, c ->
            val matchingParamsWithValue = paramsWithColumnMappers.associateBy({ it.first }, { it.second?.map(r, it.first.name, c) })
            try {
                constructor.isAccessible = true
                constructor.callBy(matchingParamsWithValue)
            } catch (e: InvocationTargetException) {
                throw IllegalArgumentException("A bean, ${clazz.name} was mapped which was not instantiable", e.targetException)
            } catch (e: ReflectiveOperationException) {
                throw IllegalArgumentException("A bean, ${clazz.name} was mapped which was not instantiable", e)
            }
        }

    }

    private fun findConstructor(kClass: KClass<C>): KFunction<C> {
        return kClass.primaryConstructor ?: findSecondaryConstructor(this.kClass)
    }

    private fun findSecondaryConstructor(kClass: KClass<C>): KFunction<C> {
        if (kClass.constructors.size == 1) {
            return kClass.constructors.first();
        } else {
            throw IllegalArgumentException("A bean, ${kClass.simpleName} was mapped which was not instantiable (cannot find appropriate constructor)")
        }
    }

    fun List<KParameter>.matchColumnName(columnName: String,
                                         columnNameMatchers: List<ColumnNameMatcher>): KParameter? {
        for (parameter in this) {
            val paramName = parameter.name
            for (strategy in columnNameMatchers) {
                if (strategy.columnNameMatches(columnName, paramName)) {
                    return parameter
                }
            }
        }
        return null
    }


}