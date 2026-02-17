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
import java.util.ArrayList;
import java.util.List;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.SimpleExtensionConfigurer;
import org.jdbi.core.mapper.RowMapperFactory;
import org.jdbi.core.mapper.RowMappers;
import org.jdbi.core.mapper.reflect.FieldMapper;
import org.jdbi.sqlobject.config.RegisterFieldMapper;
import org.jdbi.sqlobject.config.RegisterFieldMappers;

public class RegisterFieldMappersImpl extends SimpleExtensionConfigurer {

    private final List<RowMapperFactory> fieldMappers;

    public RegisterFieldMappersImpl(Annotation annotation) {
        RegisterFieldMappers registerFieldMappers = (RegisterFieldMappers) annotation;
        this.fieldMappers = new ArrayList<>(registerFieldMappers.value().length);

        for (RegisterFieldMapper registerFieldMapper : registerFieldMappers.value()) {
            Class<?> fieldClass = registerFieldMapper.value();
            String prefix = registerFieldMapper.prefix();

            if (prefix.isEmpty()) {
                this.fieldMappers.add(FieldMapper.factory(fieldClass));
            } else {
                this.fieldMappers.add(FieldMapper.factory(fieldClass, prefix));
            }
        }
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        RowMappers rowMappers = config.get(RowMappers.class);
        fieldMappers.forEach(rowMappers::register);
    }
}
