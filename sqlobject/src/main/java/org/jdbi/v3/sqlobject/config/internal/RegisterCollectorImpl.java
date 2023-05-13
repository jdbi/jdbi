package org.jdbi.v3.sqlobject.config.internal;

import org.jdbi.v3.core.collector.JdbiCollectors;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.SimpleExtensionConfigurer;
import org.jdbi.v3.sqlobject.config.RegisterCollector;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;

public class RegisterCollectorImpl extends SimpleExtensionConfigurer {
    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> extensionType) {
        RegisterCollector registerCollector = (RegisterCollector) annotation;
        JdbiCollectors mappers = config.get(JdbiCollectors.class);
        try {
            Class<?> type = (Class<?>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[2]; //gets resultant type of collector
            mappers.registerCollector(type.getGenericSuperclass(), registerCollector.value().getConstructor().newInstance());
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new IllegalStateException("Unable to instantiate collector class " + registerCollector.value(), e);
        }
    }
}
