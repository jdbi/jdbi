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
import org.jdbi.core.internal.JdbiClassUtils;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.mapper.RowMappers;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.config.RegisterRowMappers;

public class RegisterRowMappersImpl extends SimpleExtensionConfigurer {

    private final List<RowMapper<?>> rowMappers;

    public RegisterRowMappersImpl(Annotation annotation) {
        final RegisterRowMappers registerRowMappers = (RegisterRowMappers) annotation;
        this.rowMappers = new ArrayList<>(registerRowMappers.value().length);

        for (RegisterRowMapper registerRowMapper : registerRowMappers.value()) {
            rowMappers.add(JdbiClassUtils.checkedCreateInstance(registerRowMapper.value()));
        }
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        RowMappers rowMappersConfig = config.get(RowMappers.class);
        rowMappers.forEach(rowMappersConfig::register);
    }
}
