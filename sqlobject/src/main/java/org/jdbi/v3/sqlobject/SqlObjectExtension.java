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

import org.jdbi.v3.Handle;
import org.jdbi.v3.extension.ExtensionFactory;

public enum SqlObjectExtension implements ExtensionFactory<SqlObjectConfig> {
    INSTANCE;

    @Override
    public SqlObjectConfig createConfig() {
        return new SqlObjectConfig();
    }

    @Override
    public boolean accepts(Class<?> extensionType) {
        return true;
    }

    /**
     * Create a sql object of the specified type bound to this handle. Any state changes to the handle, or the sql
     * object, such as transaction status, closing it, etc, will apply to both the object and the handle.
     *
     * @param extensionType the type of sql object to create
     * @param handle the Handle instance to attach ths sql object to
     * @return the new sql object bound to this handle
     */
    @Override
    public <E> E attach(Class<E> extensionType, SqlObjectConfig config, Handle handle) {
        return SqlObject.buildSqlObject(extensionType, new ConstantHandleDing(handle));
    }
}
