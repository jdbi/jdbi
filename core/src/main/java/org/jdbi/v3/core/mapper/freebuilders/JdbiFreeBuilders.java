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
package org.jdbi.v3.core.mapper.freebuilders;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.mapper.reflect.internal.BuilderPropertiesFactory;
import org.jdbi.v3.core.mapper.reflect.internal.FreeBuilderPropertiesFactory;
import org.jdbi.v3.core.mapper.reflect.internal.PojoTypes;
import org.jdbi.v3.meta.Beta;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

@Beta
public class JdbiFreeBuilders implements JdbiConfig<JdbiFreeBuilders> {
    private ConfigRegistry registry;

    public JdbiFreeBuilders() {}

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    @Override
    public JdbiFreeBuilders createCopy() {
        return new JdbiFreeBuilders();
    }

    public <S> JdbiFreeBuilders registerFreeBuilder(Class<S> spec) throws NoSuchMethodException, IllegalAccessException {
        final Class builderClass = builderClass(spec);
        final Class<? extends S> valueClass = valueClass(spec);
        return registerFreeBuilder(spec, valueClass, builderClass);
    }

    @SuppressWarnings("unchecked")
    private static <S> Supplier<S> constructorOf(Class<S> impl) {
        try {
            return (Supplier<S>) Unchecked.supplier(MethodHandles.lookup().findConstructor(impl, MethodType.methodType(void.class))::invoke);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Couldn't find public constructor of " + impl, e);
        }
    }

    private <S, B> JdbiFreeBuilders registerFreeBuilder(Class<S> spec, Class<? extends S> valueClass, Class<B> builderClass) {
        Supplier<B> builder = constructorOf(builderClass);
        return register(spec, valueClass, FreeBuilderPropertiesFactory.create(spec, valueClass, builder));
    }

    private <S> JdbiFreeBuilders register(Class<S> spec, Class<? extends S> valueClass, BuilderPropertiesFactory factory) {
        registry.get(PojoTypes.class).register(spec, factory).register(valueClass, factory);
        return this;
    }

    private <S> Class builderClass(Class<S> spec) {
        final String builderName;
        if (spec.getEnclosingClass() != null) {
            String enclosingName = spec.getEnclosingClass().getSimpleName();
            builderName = spec.getPackage().getName() + "." + enclosingName + "$" + spec.getSimpleName() + "$" + "Builder";
        } else {
            builderName = spec.getPackage().getName() + "." + spec.getSimpleName() + "." + "Builder";
        }

        try {
            return Class.forName(builderName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Couldn't locate builder class "+ builderName, e);
        }
    }

    private <S> Class<? extends S> valueClass(Class<S> spec) {
        final String valueName;
        if (spec.getEnclosingClass() != null) {
            String enclosingName = spec.getEnclosingClass().getSimpleName();
            valueName = spec.getPackage().getName() + "." + enclosingName + "_" + spec.getSimpleName() + "_" + "Builder" + "$" + "Value";
        } else {
            valueName = spec.getPackage().getName() + "." + spec.getSimpleName() + "_" + "Builder" + "$" + "Value";
        }
        try {
            return Class.forName(valueName).asSubclass(spec);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Couldn't locate value class "+ valueName, e);
        }
    }
}
