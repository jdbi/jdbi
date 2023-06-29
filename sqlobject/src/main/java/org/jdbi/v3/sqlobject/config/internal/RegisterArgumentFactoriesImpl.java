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

import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.SimpleExtensionConfigurer;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactories;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;

public class RegisterArgumentFactoriesImpl extends SimpleExtensionConfigurer {

    private final List<ArgumentFactory> argumentFactories;

    public RegisterArgumentFactoriesImpl(Annotation annotation) {
        final RegisterArgumentFactories registerArgumentFactories = (RegisterArgumentFactories) annotation;
        this.argumentFactories = new ArrayList<>(registerArgumentFactories.value().length);

        for (RegisterArgumentFactory registerArgumentFactory : registerArgumentFactories.value()) {
            final Class<? extends ArgumentFactory> klass = registerArgumentFactory.value();

            try {
                argumentFactories.add(klass.getConstructor().newInstance());
            } catch (ReflectiveOperationException | SecurityException e) {
                throw new IllegalStateException("Unable to instantiate column mapper class " + klass, e);
            }
        }
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        Arguments arguments = config.get(Arguments.class);
        argumentFactories.forEach(arguments::register);
    }
}
