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
package org.jdbi.v3.sqlobject.config.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.SimpleExtensionConfigurer;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.config.RegisterColumnMappers;

public class RegisterColumnMappersImpl extends SimpleExtensionConfigurer {

    private final List<ColumnMapper<?>> columnMappers;

    public RegisterColumnMappersImpl(Annotation annotation) {
        final RegisterColumnMappers registerColumnMappers = (RegisterColumnMappers) annotation;
        this.columnMappers = new ArrayList<>(registerColumnMappers.value().length);

        for (RegisterColumnMapper registerColumnMapper : registerColumnMappers.value()) {
            columnMappers.add(JdbiClassUtils.checkedCreateInstance(registerColumnMapper.value()));
        }
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        ColumnMappers columnMappersConfig = config.get(ColumnMappers.class);
        columnMappers.forEach(columnMappersConfig::register);
    }
}
