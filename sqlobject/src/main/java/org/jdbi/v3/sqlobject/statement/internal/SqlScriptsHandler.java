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
package org.jdbi.v3.sqlobject.statement.internal;

import java.lang.reflect.Method;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Script;

public class SqlScriptsHandler extends AbstractCustomizingStatementHandler<Script> {

    public SqlScriptsHandler(Class<?> sqlObjectType, Method method) {
        super(sqlObjectType, method);
    }

    @Override
    void configureReturner(Script stmt, SqlObjectStatementConfiguration cfg) {
        cfg.setReturner(() -> stmt.execute());
    }

    @Override
    Script createStatement(Handle handle, String locatedSql) {
        return new Script(handle, locatedSql);
    }
}
