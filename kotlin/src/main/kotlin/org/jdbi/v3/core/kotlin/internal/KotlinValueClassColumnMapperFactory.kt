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

import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.mapper.ColumnMapperFactory
import org.jdbi.v3.core.mapper.ColumnMappers
import org.jdbi.v3.core.mapper.NoSuchMapperException
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.meta.Alpha
import java.lang.reflect.Type
import java.sql.ResultSet
import java.util.Optional
import kotlin.reflect.full.primaryConstructor

/**
 * A [ColumnMapperFactory] which supports Kotlin value classes.
 */
@Alpha
class KotlinValueClassColumnMapperFactory : ColumnMapperFactory {
    override fun build(type: Type, config: ConfigRegistry): Optional<ColumnMapper<*>> {
        val clazz = (type as? Class<*>)?.kotlin?.also {
            if (!it.isValue) return Optional.empty()
        } ?: return Optional.empty()

        val constructor = clazz.primaryConstructor ?: return Optional.empty()
        val valueParameterType = clazz.primaryConstructor?.parameters?.singleOrNull()?.type ?: return Optional.empty()
        val valueParameterJavaType = toJavaType(valueParameterType)

        val innerMapper = config[ColumnMappers::class.java].findFor(valueParameterJavaType).orElseThrow {
            NoSuchMapperException("No column mapper registered for parameter (kotlin: $valueParameterType, java: $valueParameterJavaType) of type $type")
        }

        return Optional.of(
            object : ColumnMapper<Any?> {
                override fun map(rs: ResultSet, columnNumber: Int, ctx: StatementContext): Any? {
                    val innerValue = innerMapper.map(rs, columnNumber, ctx)
                    return if (!rs.wasNull()) constructor.call(innerValue) else null
                }

                override fun toString() = "KotlinValueClassColumnMapper(kClass = $clazz)"
            }
        )
    }

    override fun toString() = "KotlinValueClassColumnMapperFactory"
}
