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

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.SimpleExtensionConfigurer;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;

public class RegisterConstructorMapperImpl extends SimpleExtensionConfigurer {

    private final RowMapperFactory constructorMapper;

    public RegisterConstructorMapperImpl(Annotation annotation) {
        RegisterConstructorMapper registerConstructorMapper = (RegisterConstructorMapper) annotation;
        constructorMapper = createFactory(registerConstructorMapper);
    }

    static RowMapperFactory createFactory(RegisterConstructorMapper annotation) {
        Class<?> constructorClass = annotation.value();
        String prefix = annotation.prefix();
        Class<?> factoryMethodClass = annotation.usingStaticMethodIn();
        if (void.class.equals(factoryMethodClass)) {
            return ConstructorMapper.factory(constructorClass, prefix);
        } else {
            return ConstructorMapper.factory(constructorClass, factoryMethodClass, prefix);
        }
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        RowMappers mappers = config.get(RowMappers.class);
        mappers.register(constructorMapper);
    }
}
