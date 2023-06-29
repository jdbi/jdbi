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
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapperFactories;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapperFactory;

public class RegisterColumnMapperFactoriesImpl extends SimpleExtensionConfigurer {

    private final List<ColumnMapperFactory> columnMapperFactories;

    public RegisterColumnMapperFactoriesImpl(Annotation annotation) {
        final RegisterColumnMapperFactories registerColumnMapperFactories = (RegisterColumnMapperFactories) annotation;
        this.columnMapperFactories = new ArrayList<>(registerColumnMapperFactories.value().length);

        for (RegisterColumnMapperFactory registerColumnMapperFactory : registerColumnMapperFactories.value()) {
            final Class<? extends ColumnMapperFactory> klass = registerColumnMapperFactory.value();

            try {
                columnMapperFactories.add(klass.getConstructor().newInstance());
            } catch (ReflectiveOperationException | SecurityException e) {
                throw new IllegalStateException("Unable to instantiate row mapper factory class " + klass, e);
            }
        }
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        ColumnMappers columnMappers = config.get(ColumnMappers.class);
        columnMapperFactories.forEach(columnMappers::register);
    }
}
