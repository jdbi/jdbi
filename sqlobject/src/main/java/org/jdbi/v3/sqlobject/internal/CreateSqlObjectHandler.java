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
package org.jdbi.v3.sqlobject.internal;

import java.lang.reflect.Method;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.AttachedExtensionHandler;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.ExtensionHandler;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.internal.OnDemandExtensions;
import org.jdbi.v3.core.internal.OnDemandHandleSupplier;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.SqlObjectFactory;

public class CreateSqlObjectHandler implements ExtensionHandler {
    private final Method method;

    public CreateSqlObjectHandler(Class<?> sqlObjectType, Method method) {
        this.method = method;
    }

    @Override
    public AttachedExtensionHandler attachTo(ConfigRegistry config, Object target) {
        OnDemandExtensions onDemand = config.get(OnDemandExtensions.class);
        Optional<ExtensionFactory> sqlObjectFactory = config.get(Extensions.class).findFactory(SqlObjectFactory.class);
        return new AttachedExtensionHandler() {
            @Override
            public Object invoke(HandleSupplier handleSupplier, Object... args) {
                if (handleSupplier instanceof OnDemandHandleSupplier) {
                    return onDemand.create(handleSupplier.getJdbi(), method.getReturnType(), SqlObject.class);
                }
                return sqlObjectFactory
                        .orElseThrow(() -> new IllegalStateException("Can't locate SqlObject factory"))
                        .attach(method.getReturnType(), handleSupplier);
            }
        };
    }
}
