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

import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.ObjectArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.sqlobject.config.RegisterObjectArgumentFactory;

public class RegisterObjectArgumentFactoryImpl implements Configurer
{
    @Override
    public void configureForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType)
    {
        RegisterObjectArgumentFactory registerObjectArgumentFactory = (RegisterObjectArgumentFactory) annotation;
        Arguments arguments = registry.get(Arguments.class);

        Class<?>[] classes = registerObjectArgumentFactory.value();
        int[] sqlTypes = registerObjectArgumentFactory.sqlType();

        if (sqlTypes.length != 0 && sqlTypes.length != classes.length) {
            throw new IllegalStateException("RegisterObjectArgumentFactory.sqlTypes() must have the same number of elements as value()");
        }

        for (int i = 0; i < classes.length; i++) {
            Class<?> clazz = classes[i];
            Integer sqlType = sqlTypes.length == 0 ? null : sqlTypes[i];

            arguments.register(ObjectArgumentFactory.create(clazz, sqlType));
        }
    }

    @Override
    public void configureForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method)
    {
        configureForType(registry, annotation, sqlObjectType);
    }
}
