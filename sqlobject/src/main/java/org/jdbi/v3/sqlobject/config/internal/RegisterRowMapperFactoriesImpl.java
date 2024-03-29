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
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.sqlobject.config.RegisterRowMapperFactories;
import org.jdbi.v3.sqlobject.config.RegisterRowMapperFactory;

public class RegisterRowMapperFactoriesImpl extends SimpleExtensionConfigurer {

    private final List<RowMapperFactory> rowMapperFactories;

    public RegisterRowMapperFactoriesImpl(Annotation annotation) {
        final RegisterRowMapperFactories registerRowMapperFactories = (RegisterRowMapperFactories) annotation;
        this.rowMapperFactories = new ArrayList<>(registerRowMapperFactories.value().length);

        for (RegisterRowMapperFactory registerRowMapperFactory : registerRowMapperFactories.value()) {

            rowMapperFactories.add(JdbiClassUtils.checkedCreateInstance(registerRowMapperFactory.value()));
        }
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        RowMappers rowMappers = config.get(RowMappers.class);
        rowMapperFactories.forEach(rowMappers::register);
    }
}
