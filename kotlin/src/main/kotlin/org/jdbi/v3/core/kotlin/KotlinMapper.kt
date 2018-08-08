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

import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.mapper.SingleColumnMapper
import org.jdbi.v3.core.mapper.reflect.ColumnName
import org.jdbi.v3.core.mapper.reflect.ColumnNameMatcher
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor
import org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.*
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.util.OptionalInt
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

private val nullValueRowMapper = RowMapper<Any?> { rs, ctx -> null }

class KotlinMapper(clazz: Class<*>, private val prefix: String = "") : RowMapper<Any> {
    private val kClass: KClass<*> = clazz.kotlin
    private val constructor = findConstructor(kClass)
    private val constructorParameters = constructor.parameters
    private val memberProperties = kClass.memberProperties.mapNotNull { it as? KMutableProperty1<*, *>}.filter { property ->
        !constructorParameters.any { parameter -> parameter.paramName() == property.propName() }
    }

    private val nestedMappers = ConcurrentHashMap<KParameter, KotlinMapper>()
    private val nestedPropertyMappers = ConcurrentHashMap<KMutableProperty1<*, *>, KotlinMapper>()

    override fun map(rs: ResultSet, ctx: StatementContext): Any {
        return specialize(rs, ctx).map(rs, ctx)
    }

    override fun specialize(rs: ResultSet, ctx: StatementContext): RowMapper<Any> {
        val columnNames = getColumnNames(rs)
        val columnNameMatchers = ctx.getConfig(ReflectionMappers::class.java).columnNameMatchers
        val unmatchedColumns = columnNames.toMutableSet()

        val mapper = specialize0(rs, ctx, columnNames, columnNameMatchers, unmatchedColumns)

        if (ctx.getConfig(ReflectionMappers::class.java).isStrictMatching &&
                unmatchedColumns.any { col -> col.startsWith(prefix) }) {

            throw IllegalArgumentException(String.format(
                    "Mapping constructor-injected type %s could not match parameters for columns: %s",
                    kClass.simpleName,
                    unmatchedColumns))
        }

        return mapper
    }

    private fun specialize0(rs: ResultSet,
                            ctx: StatementContext,
                            columnNames: List<String>,
                            columnNameMatchers: List<ColumnNameMatcher>,
                            unmatchedColumns: MutableSet<String>
    ): RowMapper<Any> {
        val constructorParameterMappers = constructorParameters.associate { parameter ->
            parameter to getConstructorParameterProvider(rs, ctx, parameter, columnNames, columnNameMatchers, unmatchedColumns)
        }

        val memberPropertyMappers = memberProperties.flatMap { property ->
            val mapper  = getMemberPropertyProvider(rs, ctx, property, columnNames, columnNameMatchers, unmatchedColumns)
            if (mapper != null) listOf(Pair(property, mapper)) else emptyList()
        } .toMap()

        return RowMapper { r, c ->
            // We filter 'null' mappers to remove parameters with no mappers but a default value
            val constructorParametersWithValues = constructorParameterMappers.filterValues { it != null }.mapValues { (_, mapper) ->
                mapper?.map(r, c)
            }

            val memberPropertiesWithValues = memberPropertyMappers.mapValues { (_, mapper) ->
                mapper.map(r, c)
            }

            constructor.isAccessible = true
            constructor.callBy(constructorParametersWithValues).also { instance ->
                memberPropertiesWithValues.forEach { (prop, value) ->
                    prop.isAccessible = true
                    prop.setter.call(instance, value)
                }
            }
        }
    }

    private fun getConstructorParameterProvider(rs: ResultSet,
                                                ctx: StatementContext,
                                                parameter: KParameter,
                                                columnNames: List<String>,
                                                columnNameMatchers: List<ColumnNameMatcher>,
                                                unmatchedColumns: MutableSet<String>
    ): RowMapper<*>? {
        val parameterName = parameter.paramName()

        val nested = parameter.findAnnotation<Nested>()

        return if (nested == null) {
            val columnIndex = findColumnIndex(parameterName, columnNames, columnNameMatchers, { parameter.name })
            when {
                columnIndex.isPresent -> {
                    val type = parameter.type.javaType

                    ctx.findColumnMapperFor(type)
                            .map { mapper -> SingleColumnMapper(mapper, columnIndex.asInt + 1) }
                            .orElseThrow {
                                IllegalArgumentException(
                                        "Could not find column mapper for type '$type' of parameter " +
                                                "'$parameter' for constructor '$constructor'")
                            }.also {
                        unmatchedColumns.remove(columnNames[columnIndex.asInt])
                    }
                }
                parameter.isOptional -> {
                    // Parameter has no matching column but has a default value, use the default value
                    null
                }
                parameter.type.isMarkedNullable -> nullValueRowMapper
                else -> throw IllegalArgumentException(
                        "Constructor '${constructor.name}' parameter '$parameterName' has no column in the result set" +
                                " and is not nullable. " +
                                "Verify that your result set has the columns expected, or annotate the " +
                                "parameter names explicitly with @ColumnName"
                )
            }
        } else {
            val nestedPrefix = prefix + nested.value

            nestedMappers
                    .computeIfAbsent(parameter, { p -> KotlinMapper(p.type.jvmErasure.java, nestedPrefix) })
                    .specialize0(rs, ctx, columnNames, columnNameMatchers, unmatchedColumns)
        }
    }

    private fun getMemberPropertyProvider(rs: ResultSet,
                                          ctx: StatementContext,
                                          property: KMutableProperty1<*, *>,
                                          columnNames: List<String>,
                                          columnNameMatchers: List<ColumnNameMatcher>,
                                          unmatchedColumns: MutableSet<String>
    ): RowMapper<*>? {
        val propertyName = property.propName()
        val nested = property.javaField?.getAnnotation(Nested::class.java)

        if (nested == null) {
            val possibleColumnIndex : OptionalInt = findColumnIndex(propertyName, columnNames, columnNameMatchers, { property.name })
            val columnIndex : Int = when {
                possibleColumnIndex.isPresent -> possibleColumnIndex.asInt
                ! property.isLateinit -> return null
                else -> throw IllegalArgumentException(
                    "Member '${property.name}' of class '${kClass.simpleName} has no column in the result set but is lateinit. " +
                        "Verify that your result set has the columns expected, or annotate the " +
                        "property explicitly with @ColumnName"
                )
            }

            val type = property.returnType.javaType
            return ctx.findColumnMapperFor(type)
                    .map { mapper -> SingleColumnMapper(mapper, columnIndex + 1) }
                    .orElseThrow {
                        IllegalArgumentException(
                                "Could not find column mapper for type '$type' of property " +
                                        "'${property.name}' for constructor '${kClass.simpleName}'")
                    }
                    .also {
                        unmatchedColumns.remove(columnNames[columnIndex])
                    }
        } else {
            val nestedPrefix = prefix + nested.value

            return nestedPropertyMappers
                    .computeIfAbsent(property, { p -> KotlinMapper(p.returnType.jvmErasure.java, nestedPrefix) })
                    .specialize0(rs, ctx, columnNames, columnNameMatchers, unmatchedColumns)
        }
    }

    private fun KParameter.paramName(): String? {
        return prefix + (findAnnotation<ColumnName>()?.value ?: name)
    }

    private fun KMutableProperty1<*, *>.propName(): String {
        val annotation = this.javaField?.getAnnotation(ColumnName::class.java)
        return prefix + (annotation?.value ?: name)
    }
}

private fun <C : Any> findConstructor(kClass: KClass<C>) : KFunction<C> {
    val annotatedConstructors = kClass.constructors.filter { it.findAnnotation<JdbiConstructor>() != null }
    return when {
        annotatedConstructors.isEmpty() -> kClass.primaryConstructor ?: findSecondaryConstructor(kClass)
        annotatedConstructors.size == 1 -> annotatedConstructors.first()
        else -> throw IllegalArgumentException("A bean, ${kClass.simpleName} was mapped which was not instantiable (multiple constructors marked with ${JdbiConstructor::class.simpleName})")
    }
}

private fun <C : Any> findSecondaryConstructor(kClass: KClass<C>): KFunction<C> {
    if (kClass.constructors.size == 1) {
        return kClass.constructors.first()
    } else {
        throw IllegalArgumentException("A bean, ${kClass.simpleName} was mapped which was not instantiable (cannot find appropriate constructor)")
    }
}
