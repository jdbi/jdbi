package org.jdbi.v3.core.kotlin

import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.generic.GenericTypes.getErasedType
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.mapper.RowMapperFactory
import java.lang.reflect.Type
import java.util.*

class KotlinMapperFactory : RowMapperFactory {

    override fun build(type: Type, config: ConfigRegistry): Optional<RowMapper<*>> {
        val erasedType = getErasedType(type);

        return when (erasedType.isKotlinClass()) {
            true -> Optional.of(KotlinMapper(erasedType))
            false -> Optional.empty()
        }
    }
}