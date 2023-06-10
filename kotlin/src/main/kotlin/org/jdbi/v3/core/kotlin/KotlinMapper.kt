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

import org.jdbi.v3.core.annotation.internal.JdbiAnnotations
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.mapper.SingleColumnMapper
import org.jdbi.v3.core.mapper.reflect.ColumnName
import org.jdbi.v3.core.mapper.reflect.ColumnNameMatcher
import org.jdbi.v3.core.mapper.reflect.FieldMapper
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor
import org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.addPropertyNamePrefix
import org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.anyColumnsStartWithPrefix
import org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.findColumnIndex
import org.jdbi.v3.core.mapper.reflect.ReflectionMapperUtil.getColumnNames
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers
import org.jdbi.v3.core.mapper.reflect.internal.NullDelegatingMapper
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.util.Optional
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

class KotlinMapper(val kClass: KClass<*>, private val prefix: String = "") : RowMapper<Any> {
    private val constructor = findConstructor(kClass)
    private val constructorParameters = constructor.parameters
    private val memberProperties = kClass.memberProperties
        .mapNotNull { it as? KMutableProperty1<*, *> }
        .filter { property ->
            !constructorParameters.any { parameter -> parameter.paramName() == property.propName() }
        }

    private val nestedMappers = ConcurrentHashMap<KParameter, KotlinMapper>()
    private val nestedPropertyMappers = ConcurrentHashMap<KMutableProperty1<*, *>, KotlinMapper>()

    constructor(clazz: Class<*>, prefix: String = "") : this(clazz.kotlin, prefix)

    override fun map(rs: ResultSet, ctx: StatementContext): Any? = specialize(rs, ctx).map(rs, ctx)

    override fun specialize(rs: ResultSet, ctx: StatementContext): RowMapper<Any?> {
        val caseChange = ctx.getConfig(ReflectionMappers::class).caseChange
        val columnNames = getColumnNames(rs, caseChange)
        val columnNameMatchers = ctx.getConfig(ReflectionMappers::class).columnNameMatchers
        val unmatchedColumns = columnNames.toMutableSet()

        val mapper = createSpecializedRowMapper(
            ctx = ctx,
            columnNames = columnNames,
            columnNameMatchers = columnNameMatchers,
            unmatchedColumns = unmatchedColumns
        ).orElseGet {
            RowMapper<Any?> { _, _ ->
                throw IllegalArgumentException(
                    "Could not match constructor parameters $unmatchedColumns for ${kClass.simpleName()}, prefix: $prefix"
                )
            }
        }

        require(!(ctx.getConfig(ReflectionMappers::class).isStrictMatching && anyColumnsStartWithPrefix(unmatchedColumns, prefix, columnNameMatchers))) {
            "Mapping Kotlin type ${kClass.simpleName()} could not match parameters for columns: $unmatchedColumns"
        }

        return mapper
    }

    private fun createSpecializedRowMapper(
        ctx: StatementContext,
        columnNames: List<String>,
        columnNameMatchers: List<ColumnNameMatcher>,
        unmatchedColumns: MutableSet<String>
    ): Optional<RowMapper<Any?>> {
        val resolvedConstructorParameters = constructorParameters
            .map { parameter ->
                parameter to resolveConstructorParameterMapper(
                    ctx = ctx,
                    parameter = parameter,
                    columnNames = columnNames,
                    columnNameMatchers = columnNameMatchers,
                    unmatchedColumns = unmatchedColumns
                )
            }
            .sortedBy { if (it.second.propagateNull) 0 else 1 }
            .associate { it }

        val explicitlyMappedConstructorParameters = resolvedConstructorParameters
            .filter { it.value.type == ParamResolution.MAPPED }
            .keys

        val unmappedConstructorParameters = resolvedConstructorParameters
            .filter { it.value.type == ParamResolution.UNMAPPED }
            .keys

        if (unmappedConstructorParameters.isNotEmpty()) {
            if (explicitlyMappedConstructorParameters.isEmpty()) {
                // at least one constructor parameter is unmapped, and the rest are defaulted or nullable
                return Optional.empty()
            }
            // some constructor parameters explicitly mapped, and some unmapped
            throw IllegalArgumentException(
                @Suppress("MaxLineLength")
                """
                Could not map Constructor-injected type '${kClass.simpleName}' with ${constructorParameters.size} parameters, only ${explicitlyMappedConstructorParameters.size} parameter matched, ${unmappedConstructorParameters.size} unmatched.
                Available columns:    $columnNames
                Matched parameters:   $explicitlyMappedConstructorParameters
                Unmatched parameters: $unmappedConstructorParameters
                """.trimIndent()
            )
        }

        val memberPropertyMappers = memberProperties.filter {
            JdbiAnnotations.isMapped(it.javaField)
        }.associateWith { property ->
            ParamData(
                ParamResolution.MAPPED,
                resolveMemberPropertyMapper(
                    ctx = ctx,
                    property = property,
                    columnNames = columnNames,
                    columnNameMatchers = columnNameMatchers,
                    unmatchedColumns = unmatchedColumns
                ),
                property.javaField != null && FieldMapper.checkPropagateNullAnnotation(property.javaField)
            )
        }

        if (explicitlyMappedConstructorParameters.isEmpty() && memberPropertyMappers.isEmpty()) {
            // no constructor parameters or properties are mapped. nothing for us to do
            return Optional.empty()
        }

        val boundMapper = this.BoundKotlinMapper(resolvedConstructorParameters, memberPropertyMappers)

        val propagateNullColumnIndex = locatePropagateNullColumnIndex(columnNames, columnNameMatchers)

        return if (propagateNullColumnIndex.isPresent) {
            Optional.of(NullDelegatingMapper(propagateNullColumnIndex.asInt + 1, boundMapper))
        } else {
            Optional.of(boundMapper)
        }
    }

    private fun locatePropagateNullColumnIndex(
        columnNames: List<String>,
        columnNameMatchers: List<ColumnNameMatcher>
    ): OptionalInt {
        val propagateNullColumn =
            Optional.ofNullable(kClass.findAnnotation<PropagateNull>())
                .map(PropagateNull::value)
                .map { name -> addPropertyNamePrefix(prefix, name) }

        if (!propagateNullColumn.isPresent) {
            return OptionalInt.empty()
        }

        return findColumnIndex(propagateNullColumn.get(), columnNames, columnNameMatchers, propagateNullColumn::get)
    }

    private fun resolveConstructorParameterMapper(
        ctx: StatementContext,
        parameter: KParameter,
        columnNames: List<String>,
        columnNameMatchers: List<ColumnNameMatcher>,
        unmatchedColumns: MutableSet<String>
    ): ParamData {
        val nested = parameter.findAnnotation<Nested>()

        @Suppress("BooleanPropertyNaming")
        val propagateNull = checkPropagateNullAnnotation(parameter)
        if (nested == null) {
            val parameterName = addPropertyNamePrefix(prefix, parameter.paramName())
            val columnIndex = findColumnIndex(parameterName, columnNames, columnNameMatchers) { parameter.name }
            if (columnIndex.isPresent) {
                val type = QualifiedType.of(parameter.type.javaType)
                    .withAnnotations(getQualifiers(parameter))

                return ctx.findColumnMapperFor(type)
                    .map { mapper ->
                        ParamData(ParamResolution.MAPPED, SingleColumnMapper(mapper, columnIndex.asInt + 1), propagateNull)
                    }
                    .orElseThrow {
                        IllegalArgumentException(
                            "Could not find column mapper for type '$type' of parameter '$parameterName' for constructor '$constructor'"
                        )
                    }.also {
                        unmatchedColumns.remove(columnNames[columnIndex.asInt])
                    }
            }
        } else {
            val nestedPrefix = addPropertyNamePrefix(prefix, nested.value)

            if (anyColumnsStartWithPrefix(columnNames, nestedPrefix, columnNameMatchers)) {
                val nestedMapper = nestedMappers
                    .computeIfAbsent(parameter) { p ->
                        KotlinMapper(p.type.jvmErasure.java, nestedPrefix)
                    }
                    .createSpecializedRowMapper(
                        ctx = ctx,
                        columnNames = columnNames,
                        columnNameMatchers = columnNameMatchers,
                        unmatchedColumns = unmatchedColumns
                    )
                if (nestedMapper.isPresent) {
                    return ParamData(ParamResolution.MAPPED, nestedMapper.get(), propagateNull)
                }
            }
        }

        // parameter does not resolve to any column. Let's figure out what to do.
        if (parameter.isOptional || parameter.type.isMarkedNullable) {
            return ParamData(
                if (parameter.type.isMarkedNullable) ParamResolution.USE_DEFAULT else ParamResolution.MAPPED,
                null,
                propagateNull
            )
        }

        return ParamData(ParamResolution.UNMAPPED, null, propagateNull)
    }

    private fun resolveMemberPropertyMapper(
        ctx: StatementContext,
        property: KMutableProperty1<*, *>,
        columnNames: List<String>,
        columnNameMatchers: List<ColumnNameMatcher>,
        unmatchedColumns: MutableSet<String>
    ): RowMapper<*>? {
        val nested = property.javaField?.getAnnotation(Nested::class.java)?.value

        if (nested == null) {
            val propertyName = addPropertyNamePrefix(prefix, property.propName())
            val possibleColumnIndex: OptionalInt = findColumnIndex(propertyName, columnNames, columnNameMatchers) { property.name }
            val columnIndex: Int = when {
                possibleColumnIndex.isPresent -> possibleColumnIndex.asInt
                !property.isLateinit -> return null
                else -> throw IllegalArgumentException(
                    "Member '${property.name}' of class '${kClass.simpleName()} has no column in the result set but is lateinit. " +
                        "Verify that your result set has the columns expected, or annotate the property explicitly with @ColumnName"
                )
            }

            val type = property.returnType.javaType
            return ctx.findColumnMapperFor(type)
                .map { mapper -> SingleColumnMapper(mapper, columnIndex + 1) }
                .orElseThrow {
                    IllegalArgumentException(
                        "Could not find column mapper for type '$type' of property '${property.name}' for constructor '${kClass.simpleName()}'"
                    )
                }
                .also {
                    unmatchedColumns.remove(columnNames[columnIndex])
                }
        } else {
            val nestedPrefix = addPropertyNamePrefix(prefix, nested)

            if (anyColumnsStartWithPrefix(columnNames, nestedPrefix, columnNameMatchers)) {
                return nestedPropertyMappers
                    .computeIfAbsent(property) { p -> KotlinMapper(p.returnType.jvmErasure.java, nestedPrefix) }
                    .createSpecializedRowMapper(
                        ctx = ctx,
                        columnNames = columnNames,
                        columnNameMatchers = columnNameMatchers,
                        unmatchedColumns = unmatchedColumns
                    )
                    .orElse(null)
            }
        }

        return null
    }

    private fun KParameter.paramName(): String? = findAnnotation<ColumnName>()?.value ?: name

    private fun KMutableProperty1<*, *>.propName(): String = this.javaField?.getAnnotation(ColumnName::class.java)?.value ?: name

    private enum class ParamResolution {
        USE_DEFAULT,
        MAPPED,
        UNMAPPED
    }

    private fun checkPropagateNullAnnotation(parameter: KParameter): Boolean {
        val propagateNullValue = Optional.ofNullable(parameter.findAnnotation<PropagateNull>()).map(PropagateNull::value)
        propagateNullValue.ifPresent { v: String ->
            require(v.isEmpty()) { "@PropagateNull does not support a value ($v) on a constructor parameter ($parameter.name)" }
        }
        return propagateNullValue.isPresent
    }

    private data class ParamData(
        val type: ParamResolution,
        val mapper: RowMapper<*>?,
        val propagateNull: Boolean
    )

    override fun toString() = "KotlinMapper(kClass=$kClass, prefix='$prefix')"

    private inner class BoundKotlinMapper(
        private val resolvedConstructorParameters: Map<KParameter, ParamData>,
        private val memberPropertyMappers: Map<KMutableProperty1<*, *>, ParamData>
    ) : RowMapper<Any?> {
        override fun map(rs: ResultSet, ctx: StatementContext): Any? {
            val constructorParametersWithValues = resolvedConstructorParameters
                .mapValues { (k, v) ->
                    // if a parameter is not required and no mapper has been given,
                    // flag it for deletion in the filter below
                    //
                    // otherwise, execute the mapper (or use a null value). That allows
                    // non-optional values without a mapper (e.g. a non-optional value that is nullable
                    // but was not mapped onto a column) to pass through the filter
                    val value = if (v.mapper == null && k.isOptional) ParamResolution.USE_DEFAULT else v.mapper?.map(rs, ctx)
                    if (value == null && v.propagateNull) {
                        return null
                    }
                    value
                }
                // remove anything that is unmapped and optional parameters (these were flagged above)
                // or that non-null, unless the column is marked as nullable
                .filter { (k, v) -> (v != ParamResolution.USE_DEFAULT) && (v != null || (k.type.isMarkedNullable)) }

            val memberPropertiesWithValues = memberProperties
                .filter { memberPropertyMappers[it]?.mapper != null }
                .associateWith { propertyMapper ->
                    val prop = memberPropertyMappers[propertyMapper]
                    val v = prop?.mapper?.map(rs, ctx)
                    if (v == null && prop?.propagateNull == true) {
                        return null
                    }
                    v
                }
            constructor.isAccessible = true

            return constructor.callBy(constructorParametersWithValues).also { instance ->
                memberPropertiesWithValues.forEach { (prop, value) ->
                    prop.isAccessible = true
                    prop.setter.call(instance, value)
                }
            }
        }
    }
}

private fun <C : Any> findConstructor(kClass: KClass<C>): KFunction<C> {
    val annotatedConstructors = kClass.constructors.filter { it.findAnnotation<JdbiConstructor>() != null }
    return when {
        annotatedConstructors.isEmpty() -> {
            kClass.primaryConstructor ?: if (kClass.constructors.size == 1) {
                kClass.constructors.first()
            } else {
                throw IllegalArgumentException("Bean ${kClass.simpleName()} is not instantiable: no matching constructor found")
            }
        }

        annotatedConstructors.size == 1 -> annotatedConstructors.first()
        else -> throw IllegalArgumentException(
            "Bean ${kClass.simpleName()} is not instantiable: multiple constructors marked with @JdbiConstructor"
        )
    }
}
