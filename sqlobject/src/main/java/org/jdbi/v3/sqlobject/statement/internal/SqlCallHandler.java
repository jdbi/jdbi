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
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.statement.Call;
import org.jdbi.v3.core.statement.OutParameters;

public class SqlCallHandler extends CustomizingStatementHandler<Call> {
    private final BiFunction<OutParameters, Call, ?> returner;

    public SqlCallHandler(Class<?> sqlObjectType, Method method) {
        super(sqlObjectType, method);
        returner = createReturner(sqlObjectType, method);
    }

    private BiFunction<OutParameters, Call, ?> createReturner(Class<?> sqlObjectType, Method method) {
        Type returnType = GenericTypes.resolveType(method.getGenericReturnType(), sqlObjectType);
        Class<?> returnClass = GenericTypes.getErasedType(returnType);
        for (int idx = 0; idx < method.getParameterCount(); idx++) {
            final int pIdx = idx;
            Parameter p = method.getParameters()[idx];
            if (p.getType().equals(Function.class)) {
                return (op, c) -> ((Function) c.getConfig(SqlObjectStatementConfiguration.class).getArgs()[pIdx]).apply(op);
            }
            if (p.getType().equals(Consumer.class)) {
                return (op, c) -> {
                    ((Consumer) c.getConfig(SqlObjectStatementConfiguration.class).getArgs()[pIdx]).accept(op);
                    return null;
                };
            }
        }
        if (Void.TYPE.equals(returnClass)) {
            return (p, c) -> null;
        } else if (OutParameters.class.isAssignableFrom(returnClass)) {
            return (p, c) -> p;
        } else {
            throw new IllegalArgumentException("@SqlCall methods may only return null or OutParameters at present");
        }
    }

    @Override
    Call createStatement(Handle handle, String locatedSql) {
        return handle.createCall(locatedSql);
    }

    @Override
    void configureReturner(Call c, SqlObjectStatementConfiguration cfg) {
        cfg.setReturner(() -> returner.apply(c.invoke(), c));
    }
}
