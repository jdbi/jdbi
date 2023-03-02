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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.sqlobject.internal.SqlObjectInitData;
import org.jdbi.v3.sqlobject.internal.SqlObjectInitData.InContextInvoker;

import static java.lang.String.format;

/**
 * Creates implementations for SqlObject interfaces.
 */
public class SqlObjectFactory implements ExtensionFactory {

    SqlObjectFactory() {}

    @Override
    public boolean accepts(Class<?> extensionType) {

        // the sql generator only deals with interfaces
        if (!extensionType.isInterface()) {
            return false;
        }

        // ignore generator types
        if (SqlObjectInitData.isConcrete(extensionType)) {
            return false;
        }

        // extending SqlObject is ok
        if (SqlObject.class.isAssignableFrom(extensionType)) {
            return true;
        }

        // otherwise at least one method must be marked with a SqlOperation
        return Stream.of(extensionType.getMethods())
                .flatMap(m -> Stream.of(m.getAnnotations()))
                .anyMatch(a -> a.annotationType().isAnnotationPresent(SqlOperation.class));
    }

    /**
     * Create a sql object of the specified type bound to this handle. Any state changes to the handle, or the sql
     * object, such as transaction status, closing it, etc, will apply to both the object and the handle.
     *
     * @param extensionType  the type of sql object to create
     * @param handleSupplier the Handle instance to attach ths sql object to
     * @return the new sql object bound to this handle
     */
    @Override
    public <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {
        if (SqlObjectInitData.isConcrete(extensionType)) {
            throw new IllegalStateException(format("Can not process %s, it is a generated SQL object", extensionType.getSimpleName()));
        }

        final SqlObjectInitData sqlObjectInitData = SqlObjectInitData.lookup(extensionType, handleSupplier.getConfig());
        final ConfigRegistry instanceConfig = sqlObjectInitData.configureInstance(handleSupplier.getConfig().createCopy());

        instanceConfig.get(Extensions.class).onCreateProxy();

        Map<Method, InContextInvoker> handlers = new HashMap<>();
        final Object proxy = Proxy.newProxyInstance(
                extensionType.getClassLoader(),
                new Class[] {extensionType},
                (proxyInstance, method, args) -> handlers.get(method).invoke(args));

        sqlObjectInitData.forEachMethodHandler((method, handler) ->
                handlers.put(method, sqlObjectInitData.getInvoker(proxy, method, handleSupplier, instanceConfig)));
        return extensionType.cast(proxy);
    }
}
