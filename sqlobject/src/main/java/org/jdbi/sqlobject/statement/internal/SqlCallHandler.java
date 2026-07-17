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
package org.jdbi.sqlobject.statement.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jdbi.core.Handle;
import org.jdbi.core.generic.GenericTypes;
import org.jdbi.core.statement.Call;
import org.jdbi.core.statement.Customizable;
import org.jdbi.core.statement.OutParameters;

public class SqlCallHandler extends CustomizingStatementHandler {
    private final BiFunction<OutParameters, Call, ?> resultTransformer;

    public SqlCallHandler(Class<?> sqlObjectType, Method method) {
        super(sqlObjectType, method);
        resultTransformer = createResultTransformer(sqlObjectType, method);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private BiFunction<OutParameters, Call, ?> createResultTransformer(Class<?> sqlObjectType, Method method) {
        Type returnType = GenericTypes.resolveType(method.getGenericReturnType(), sqlObjectType);
        Class<?> returnClass = GenericTypes.getErasedType(returnType);
        for (int idx = 0; idx < method.getParameterCount(); idx++) {
            final int pIdx = idx;
            Parameter p = method.getParameters()[idx];
            if (p.getType().equals(Function.class)) {
                return (outParameters, call) -> ((Function) SqlObjectStatementState.from(call.getContext()).getArgs()[pIdx]).apply(outParameters);
            } else if (p.getType().equals(Consumer.class)) {
                return (outParameters, call) -> {
                    ((Consumer) SqlObjectStatementState.from(call.getContext()).getArgs()[pIdx]).accept(outParameters);
                    return null;
                };
            }
        }
        if (Void.TYPE.equals(returnClass)) {
            return (outParameters, call) -> null;
        } else if (OutParameters.class.isAssignableFrom(returnClass)) {
            return (outParameters, call) -> outParameters;
        } else {
            throw new IllegalArgumentException("@SqlCall methods may only return null or OutParameters at present");
        }
    }

    @Override
    Call createStatement(Handle handle, String locatedSql) {
        return handle.createCall(locatedSql);
    }

    @Override
    void configureReturner(Customizable<?> stmt, SqlObjectStatementState state) {
        final Call call = (Call) stmt;
        state.setReturner(() -> resultTransformer.apply(call.invoke(), call));
    }
}
