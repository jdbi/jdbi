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

import java.util.Optional;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.internal.OnDemandExtensions;
import org.jdbi.v3.sqlobject.internal.SqlObjectInitData;

import static java.lang.String.format;

/**
 * Support for generator instances (concrete classes that have been created by the Jdbi generator).
 */
public final class GeneratorSqlObjectFactory implements ExtensionFactory, OnDemandExtensions.Factory {

    GeneratorSqlObjectFactory() {}

    @Override
    public boolean accepts(Class<?> extensionType) {
        return SqlObjectInitData.isConcrete(extensionType);
    }

    /**
     * Create a sql object from a jdbi generator created class.
     *
     * @param extensionType  the type of sql object to create.
     * @param handleSupplier the Handle instance to attach this sql object to.
     * @return the new sql object bound to this handle.
     */
    @Override
    public <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {
        if (!SqlObjectInitData.isConcrete(extensionType)) {
            throw new IllegalStateException(format("Can not process %s, not generated SQL object", extensionType.getSimpleName()));
        }

        final SqlObjectInitData sqlObjectInitData = SqlObjectInitData.lookup(extensionType, handleSupplier.getConfig());
        final ConfigRegistry instanceConfig = sqlObjectInitData
                .configureInstance(handleSupplier.getConfig().createCopy());

        try {
            Class<?> klazz = Class.forName(getGeneratedClassName(extensionType));
            return extensionType.cast(klazz.getConstructor(SqlObjectInitData.class, HandleSupplier.class, ConfigRegistry.class)
                    .newInstance(sqlObjectInitData, handleSupplier, instanceConfig));
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new UnableToCreateSqlObjectException(e);
        }
    }

    @Override
    public Optional<Object> onDemand(Jdbi jdbi, Class<?> extensionType, Class<?>... extraTypes) {
        if (!SqlObjectInitData.isConcrete(extensionType)) {
            return Optional.empty();
        }

        try {
            return Optional.of(Class.forName(getOnDemandClassName(extensionType))
                    .getConstructor(Jdbi.class)
                    .newInstance(jdbi));
        } catch (ReflectiveOperationException | ExceptionInInitializerError e) {
            throw new UnableToCreateSqlObjectException(e);
        }
    }

    private String getGeneratedClassName(Class<?> extensionType) {
        return extensionType.getPackage().getName() + "." + extensionType.getSimpleName() + "Impl";
    }

    private String getOnDemandClassName(Class<?> extensionType) {
        return getGeneratedClassName(extensionType) + "$OnDemand";
    }
}
