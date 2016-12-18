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
package org.jdbi.v3.sqlobject.customizer;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds the entries of a {@code Map<String, Object>} to a SQL statement.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(BindMap.Factory.class)
public @interface BindMap
{
    /**
     * Prefix to apply to each map key. If specified, map keys will be bound as {@code prefix.key}.
     */
    String value() default "";

    /**
     * The set of map keys to bind. If specified, binds only the specified keys; any keys present in this property but
     * absent from the map will be bound as {@code null}. If not specified, all map entries are bound.
     * @return the map keys to bind.
     */
    String[] keys() default {};

    /**
     * Whether to automatically convert map keys to strings.
     * If false, {@code Map} keys must be strings, or an exception is thrown.
     * If true, any object may be the key, and it will be converted with {@link Object#toString()}.
     * @return whether keys will be implicitly converted to Strings.
     */
    boolean convertKeys() default false;

    class Factory implements SqlStatementCustomizerFactory {
        @Override
        public SqlStatementCustomizer createForParameter(Annotation a,
                                                         Class<?> sqlObjectType,
                                                         Method method,
                                                         Parameter param,
                                                         int index,
                                                         Object arg) {
            BindMap annotation = (BindMap) a;
            List<String> keys = Arrays.asList(annotation.keys());
            String prefix = annotation.value().isEmpty() ? "" : annotation.value() + ".";
            Map<?, ?> map = (Map) arg;
            Map<String, Object> toBind = new HashMap<>();
            map.forEach((k, v) -> {
                if (annotation.convertKeys() || k instanceof String) {
                    String key = k.toString();
                    if (keys.isEmpty() || keys.contains(key)) {
                        toBind.put(prefix + key, v);
                    }
                } else {
                    throw new IllegalArgumentException("Key " + k + " (of " + k.getClass() + ") must be a String");
                }
            });
            keys.forEach(key -> toBind.putIfAbsent(prefix + key, null));

            return stmt -> stmt.bindMap(toBind);
        }
    }
}
