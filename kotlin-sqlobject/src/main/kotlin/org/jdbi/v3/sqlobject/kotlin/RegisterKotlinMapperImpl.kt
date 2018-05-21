package org.jdbi.v3.sqlobject.kotlin

import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.kotlin.KotlinMapper
import org.jdbi.v3.core.mapper.RowMappers
import org.jdbi.v3.sqlobject.config.Configurer
import java.lang.reflect.Method

class RegisterKotlinMapperImpl : Configurer {

    override fun configureForType(registry: ConfigRegistry, annotation: Annotation, sqlObjectType: Class<*>) {
        val registerKotlinMapper = annotation as RegisterKotlinMapper
        val kotlinClass = registerKotlinMapper.value
        val prefix = registerKotlinMapper.prefix
        val mappers = registry.get(RowMappers::class.java)
        if (prefix.isEmpty()) {
            mappers.register(KotlinMapper.factory(kotlinClass.java))
        } else {
            mappers.register(KotlinMapper.factory(kotlinClass.java, prefix))
        }
    }

    override fun configureForMethod(registry: ConfigRegistry, annotation: Annotation, sqlObjectType: Class<*>, method: Method) {
        configureForType(registry, annotation, sqlObjectType)
    }
}
