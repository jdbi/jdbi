package org.jdbi.v3.sqlobject.kotlin

import org.jdbi.v3.sqlobject.config.ConfiguringAnnotation
import org.jdbi.v3.sqlobject.config.RegisterKotlinMappers
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import kotlin.reflect.KClass

/**
 * Registers a KotlinMapper for a specific kotlin class
 */
@Retention(RetentionPolicy.RUNTIME)
@ConfiguringAnnotation(RegisterKotlinMapperImpl::class)
@Target(ElementType.TYPE, ElementType.METHOD)
@Repeatable
@java.lang.annotation.Repeatable(RegisterKotlinMappers::class)
annotation class RegisterKotlinMapper(
    /**
     * The mapped kotlin class.
     * @return the mapped kotlin class.
     */
    val value: KClass<*>,
    /**
     * Column name prefix for the kotlin type. If omitted, defaults to no prefix.
     *
     * @return Column name prefix for the kotlin type.
     */
    val prefix: String = "")
