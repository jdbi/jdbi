package org.jdbi.v3.sqlobject.config.internal;

import org.jdbi.v3.core.collector.JdbiCollectors;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.SimpleExtensionConfigurer;
import org.jdbi.v3.sqlobject.config.RegisterCollector;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class RegisterCollectorImpl extends SimpleExtensionConfigurer {
    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> extensionType) {
        RegisterCollector registerCollector = (RegisterCollector) annotation;
        JdbiCollectors collectors = config.get(JdbiCollectors.class);

        try {
            Type type = null; //resultant type
            for(Type t : registerCollector.value().getGenericInterfaces()) {
                if(t instanceof ParameterizedType pt) {
                    if(pt.getRawType().toString().equals("interface Collector")) {
                        type = pt.getActualTypeArguments()[2];
                        break;
                    }
                }
            }
            if(type == null) throw new IllegalArgumentException("Tried to pass non-collector object to @RegisterCollector");
            collectors.registerCollector(type, registerCollector.value().getConstructor().newInstance());
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new IllegalStateException("Unable to instantiate collector class " + registerCollector.value(), e);
        }
    }
}
