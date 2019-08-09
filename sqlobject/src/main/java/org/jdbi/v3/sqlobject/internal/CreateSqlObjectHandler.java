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

import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.internal.OnDemandExtensions;
import org.jdbi.v3.core.internal.OnDemandHandleSupplier;
import org.jdbi.v3.sqlobject.Handler;
import org.jdbi.v3.sqlobject.SqlObjectFactory;

public class CreateSqlObjectHandler implements Handler {
    private final Method method;

    public CreateSqlObjectHandler(Method method) {
        this.method = method;
    }

    @Override
    public Object invoke(Object target, Object[] args, HandleSupplier handle) throws Exception {
        if (handle instanceof OnDemandHandleSupplier) {
            return OnDemandExtensions.create(handle.getJdbi(), method.getReturnType());
        }
        return handle.getConfig(Extensions.class)
                .findFactory(SqlObjectFactory.class)
                .orElseThrow(() -> new IllegalStateException("Can't locate SqlObject factory"))
                .attach(method.getReturnType(), handle);
    }
}
