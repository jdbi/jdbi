package org.jdbi.v3.sqlobject.kotlin

import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.sqlobject.config.Configurer
import org.jdbi.v3.sqlobject.config.RegisterKotlinMappers
import java.lang.reflect.Method

class RegisterKotlinMappersImpl : Configurer {

    override fun configureForType(registry: ConfigRegistry, annotation: Annotation, sqlObjectType: Class<*>) {
        val delegate = RegisterKotlinMapperImpl()

        val registerKotlinMappers = annotation as RegisterKotlinMappers
        registerKotlinMappers.value.forEach { anno -> delegate.configureForType(registry, anno, sqlObjectType) }
    }

    override fun configureForMethod(registry: ConfigRegistry, annotation: Annotation, sqlObjectType: Class<*>, method: Method) {
        configureForType(registry, annotation, sqlObjectType)
    }
}
