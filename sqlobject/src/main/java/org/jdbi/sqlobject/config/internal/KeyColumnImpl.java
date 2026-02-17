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
package org.jdbi.sqlobject.config.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.ExtensionConfigurer;
import org.jdbi.core.mapper.MapEntryMappers;
import org.jdbi.sqlobject.config.KeyColumn;

public class KeyColumnImpl implements ExtensionConfigurer {

    private final String name;

    public KeyColumnImpl(Annotation annotation) {
        KeyColumn keyColumn = (KeyColumn) annotation;
        String name = keyColumn.value();
        this.name = name.isEmpty() ? null : name;
    }

    @Override
    public void configureForMethod(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType, Method method) {
        config.get(MapEntryMappers.class).setKeyColumn(name);
    }
}
