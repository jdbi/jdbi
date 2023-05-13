package org.jdbi.v3.sqlobject.config;

import org.jdbi.v3.core.extension.annotation.UseExtensionConfigurer;
import org.jdbi.v3.sqlobject.config.internal.RegisterCollectorImpl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Collector;

/**
 * Registers specifically one collector for a sql object type
 */
@UseExtensionConfigurer(RegisterCollectorImpl.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RegisterCollector {

    /**
     * The collector instance to register
     *
     * @return the collector instance
     */
    Class<? extends Collector<?, ?, ?>> value();

}
