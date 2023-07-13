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
package org.jdbi.v3.sqlobject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionMetadata;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.internal.OnDemandExtensions;
import org.jdbi.v3.core.internal.exceptions.Sneaky;

import static java.lang.String.format;

import static org.jdbi.v3.core.extension.ExtensionFactory.FactoryFlag.DONT_USE_PROXY;

/**
 * Support for generator instances (concrete classes that have been created by the Jdbi generator).
 */
final class GeneratorSqlObjectFactory extends AbstractSqlObjectFactory implements OnDemandExtensions.Factory {

    private final ConcurrentMap<Class<?>, MethodHandle> newAttached = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, MethodHandle> newOnDemand = new ConcurrentHashMap<>();

    GeneratorSqlObjectFactory() {}

    @Override
    public boolean accepts(Class<?> extensionType) {
        return isConcrete(extensionType);
    }

    @Override
    public Set<FactoryFlag> getFactoryFlags() {
        return EnumSet.of(DONT_USE_PROXY);
    }

    /**
     * Attach a sql object from a jdbi generator created class.
     *
     * @param extensionType  the type of sql object to create.
     * @param handleSupplier the Handle instance to attach this sql object to.
     * @return the new sql object bound to this handle.
     */
    @Override
    public <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {
        if (!isConcrete(extensionType)) {
            throw new IllegalStateException(format("Can not process %s, not generated SQL object", extensionType.getSimpleName()));
        }

        ConfigRegistry config = handleSupplier.getConfig();

        final ExtensionMetadata extensionMetaData = config.get(Extensions.class).findMetadata(extensionType, this);
        final ConfigRegistry instanceConfig = extensionMetaData.createInstanceConfiguration(config);

        try {
            return (E) newAttached.computeIfAbsent(extensionType, GeneratorSqlObjectFactory::createAttacher)
                                  .invokeExact(extensionMetaData, handleSupplier, instanceConfig);
        } catch (Throwable e) {
            throw Sneaky.throwAnyway(e);
        }
    }

    private static MethodHandle createAttacher(Class<?> extensionType) {
        try {
            var ctor = MethodHandles.lookup().findConstructor(
                    Class.forName(getGeneratedClassName(extensionType)),
                    MethodType.methodType(void.class, ExtensionMetadata.class, HandleSupplier.class, ConfigRegistry.class));
            return ctor.asType(ctor.type().changeReturnType(Object.class));
        } catch (ReflectiveOperationException e) {
            throw new UnableToCreateSqlObjectException(e);
        }
    }

    @Override
    public Optional<Object> onDemand(Jdbi jdbi, Class<?> extensionType, Class<?>... extraTypes) {
        if (!isConcrete(extensionType)) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    newOnDemand.computeIfAbsent(extensionType, GeneratorSqlObjectFactory::createOnDemand)
                               .invokeExact(jdbi));
        } catch (Throwable e) {
            throw Sneaky.throwAnyway(e);
        }
    }

    private static MethodHandle createOnDemand(Class<?> extensionType) {
        try {
            var ctor = MethodHandles.lookup().findConstructor(
                    Class.forName(getOnDemandClassName(extensionType)),
                    MethodType.methodType(void.class, Jdbi.class));
            return ctor.asType(ctor.type().changeReturnType(Object.class));
        } catch (ReflectiveOperationException e) {
            throw new UnableToCreateSqlObjectException(e);
        }
    }

    private static String getGeneratedClassName(Class<?> extensionType) {
        return extensionType.getPackage().getName() + "." + extensionType.getSimpleName() + "Impl";
    }

    private static String getOnDemandClassName(Class<?> extensionType) {
        return getGeneratedClassName(extensionType) + "$OnDemand";
    }
}
