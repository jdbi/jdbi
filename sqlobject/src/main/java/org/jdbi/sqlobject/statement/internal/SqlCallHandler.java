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
import java.util.function.Supplier;

import org.jdbi.core.Handle;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.generic.GenericTypes;
import org.jdbi.core.internal.MemoizingSupplier;
import org.jdbi.core.statement.Call;
import org.jdbi.core.statement.Customizable;
import org.jdbi.core.statement.OutParameters;
import org.jdbi.core.statement.StatementTemplate;

public class SqlCallHandler extends CustomizingStatementHandler {
    private final BiFunction<OutParameters, Call, ?> resultTransformer;
    private final boolean late;

    public SqlCallHandler(Class<?> sqlObjectType, Method method) {
        super(sqlObjectType, method);
        resultTransformer = createResultTransformer(sqlObjectType, method);

        // A method whose customizer must mutate configuration per invocation runs on the classic path.
        this.late = hasLateCustomizers();
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
    Function<Handle, ? extends Customizable<?>> statementFactory(ConfigRegistry config, Supplier<String> locatedSql) {
        if (late) {
            // Classic path: a fresh Call per invocation, each with its own configuration copy.
            return super.statementFactory(config, locatedSql);
        }
        // Fast path: build one template per attach, baking configure-phase customizers into its
        // configuration snapshot once. Every call binds against a fresh, thread-confined binding.
        final Supplier<StatementTemplate> template = MemoizingSupplier.of(() -> {
            final ConfigRegistry templateConfig = config.createCopy();
            applyConfigureCustomizers(new ConfigureStatement(templateConfig));
            return new StatementTemplate(templateConfig, locatedSql.get());
        });
        return handle -> template.get().call(handle);
    }

    @Override
    void applyPerInvocationCustomizers(Customizable<?> stmt, Object[] args) {
        if (late) {
            super.applyPerInvocationCustomizers(stmt, args);
        } else {
            // Configure-phase customizers are baked into the template; only bind per invocation.
            applyCustomizers(stmt, args, Phase.BIND);
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
