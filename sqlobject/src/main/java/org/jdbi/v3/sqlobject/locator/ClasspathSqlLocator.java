package org.jdbi.v3.sqlobject.locator;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.sqlobject.internal.SqlAnnotations;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * @see org.jdbi.v3.core.locator.ClasspathSqlLocator
 */
public class ClasspathSqlLocator implements SqlLocator {
    @Override
    public String locate(Class<?> sqlObjectType, Method method, ConfigRegistry config) {
        Function<String, String> valueOrMethodNameToSql = key -> {
            String filename = key.isEmpty() ? method.getName() : key;
            return org.jdbi.v3.core.locator.ClasspathSqlLocator.findSqlOnClasspath(sqlObjectType, filename);
        };

        return SqlAnnotations.getAnnotationValue(method, valueOrMethodNameToSql)
            .orElseThrow(() -> new IllegalStateException(String.format("method %s has no query annotations", method)));
    }
}
