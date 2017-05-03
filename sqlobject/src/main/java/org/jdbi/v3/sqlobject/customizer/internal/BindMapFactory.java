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
package org.jdbi.v3.sqlobject.customizer.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.sqlobject.customizer.BindMap;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;

public class BindMapFactory implements SqlStatementCustomizerFactory {
    @Override
    public SqlStatementParameterCustomizer createForParameter(Annotation a,
                                                              Class<?> sqlObjectType,
                                                              Method method,
                                                              Parameter param,
                                                              int index,
                                                              Type type) {
        BindMap annotation = (BindMap) a;
        List<String> keys = Arrays.asList(annotation.keys());
        String prefix = annotation.value().isEmpty() ? "" : annotation.value() + ".";

        return (stmt, arg) -> {
            Map<?, ?> map = (Map<?, ?>) arg;
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
            stmt.bindMap(toBind);
        };
    }
}
