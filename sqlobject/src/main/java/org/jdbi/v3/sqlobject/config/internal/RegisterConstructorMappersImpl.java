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
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMappers;

public class RegisterConstructorMappersImpl implements Configurer
{

    @Override
    public void configureForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method)
    {
        configureForType(registry, annotation, sqlObjectType);
    }

    @Override
    public void configureForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType)
    {
        Configurer delegate = new RegisterConstructorMapperImpl();

        RegisterConstructorMappers registerConstructorMappers = (RegisterConstructorMappers) annotation;
        Stream.of(registerConstructorMappers.value()).forEach(anno -> delegate.configureForType(registry, anno, sqlObjectType));
    }
}
