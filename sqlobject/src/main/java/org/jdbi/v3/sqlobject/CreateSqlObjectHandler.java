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
import java.util.function.Supplier;

import org.jdbi.v3.Handle;

class CreateSqlObjectHandler implements Handler
{
    private final Class<?> sqlObjectTypeToCreate;
    private final SqlObject config;

    CreateSqlObjectHandler(Class<?> sqlObjectTypeToCreate, SqlObject config)
    {
        this.sqlObjectTypeToCreate = sqlObjectTypeToCreate;
        this.config = config;
    }

    @Override
    public Object invoke(Supplier<Handle> handle, Object target, Object[] args, Method method)
    {
        return SqlObjectFactory.INSTANCE.attach(sqlObjectTypeToCreate, config, handle);
    }
}
