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

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.SimpleExtensionConfigurer;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.statement.SqlExceptionHandler;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.sqlobject.config.RegisterSqlExceptionHandler;
import org.jdbi.v3.sqlobject.config.RegisterSqlExceptionHandlers;

public final class RegisterSqlExceptionHandlersImpl extends SimpleExtensionConfigurer {

    private final List<SqlExceptionHandler> handlers;

    public RegisterSqlExceptionHandlersImpl(Annotation annotation) {
        final RegisterSqlExceptionHandlers registerHandlers = (RegisterSqlExceptionHandlers) annotation;
        this.handlers = new ArrayList<>(registerHandlers.value().length);

        for (RegisterSqlExceptionHandler registerHandler : registerHandlers.value()) {
            handlers.add(JdbiClassUtils.checkedCreateInstance(registerHandler.value()));
        }
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        handlers.forEach(config.get(SqlStatements.class)::addExceptionHandler);
    }
}
