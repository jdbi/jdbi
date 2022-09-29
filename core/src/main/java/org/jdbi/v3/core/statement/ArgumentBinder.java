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
package org.jdbi.v3.core.statement;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.argument.internal.NamedArgumentFinderFactory;
import org.jdbi.v3.core.argument.internal.TypedValue;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.statement.internal.PreparedBinding;

import static java.lang.String.format;

class ArgumentBinder {
    final PreparedStatement stmt;
    final StatementContext ctx;
    final ParsedParameters params;
    final Map<QualifiedType<?>, Function<Object, Argument>> argumentFactoryByType = new HashMap<>();
    /**
     * Used to tell null from absent value in Map.getOrDefault().
     */
    private static final Object ABSENT = new Object();

    private static final class Param {
        final String name;
        final int index;

        public Param(String name, int index) {
            this.name = name;
            this.index = index;
        }
    }

    ArgumentBinder(PreparedStatement stmt, StatementContext ctx, ParsedParameters params) {
        this.stmt = stmt;
        this.ctx = ctx;
        this.params = params;
    }

    void bind(Binding binding) {
        if (params.isPositional()) {
            bindPositionalCheck(binding);
        } else {
            bindNamedCheck(binding);
        }

        if (bindCached(binding)) {
            return;
        }

        if (params.isPositional()) {
            bindPositional(binding);
        } else {
            bindNamed(binding);
        }
    }

    protected boolean bindCached(Binding binding) {
        return false;
    }

    protected void bindAndMaybeCache(Consumer<Binding> binder, Binding binding) {
        binder.accept(binding);
    }

    protected final void applyArgument(Argument arg, Param param) {
        try {
            arg.apply(param.index + 1, stmt, ctx);
        } catch (SQLException e) {
            String msg = params.isPositional()
                ? "Exception while binding positional param at (0 based) position " + param.index
                : format("Exception while binding named parameter '%s'", param.name);
            throw new UnableToCreateStatementException(msg, e, ctx);
        }
    }

    private void bind(Param param, Object value, Binding binding) {
        QualifiedType<?> valueType = typeOf(value);
        Function<Object, Argument> argFactory = argumentFactoryForType(valueType);
        bindAndMaybeCache(b -> {
            Object v = params.isPositional()
                ? b.positionals.get(param.index)
                : b.named.get(param.name);
            Argument arg = argFactory.apply(unwrap(v));
            applyArgument(arg, param);
        }, binding);
    }

    private void bindPositional(Binding binding) {
        for (int index = 0; index < params.getParameterCount(); index++) {
            Object value = binding.positionals.get(index);
            bind(new Param("?", index), value, binding);
        }
    }

    private void bindPositionalCheck(Binding binding) {
        boolean moreArgumentsProvidedThanDeclared = binding.positionals.size() != params.getParameterCount();
        if (moreArgumentsProvidedThanDeclared && !ctx.getConfig(SqlStatements.class).isUnusedBindingAllowed()) {
            throw new UnableToCreateStatementException("Superfluous positional param at (0 based) position " + params.getParameterCount(), ctx);
        }
    }

    private void bindNamed(Binding binding) {
        List<String> paramNames = params.getParameterNames();
        for (int paramIndex = 0; paramIndex < paramNames.size(); paramIndex++) {
            Param param = new Param(paramNames.get(paramIndex), paramIndex);
            Object namedValue = binding.named.getOrDefault(param.name, ABSENT);
            if (namedValue != ABSENT) {
                bind(param, namedValue, binding);
                continue;
            }

            if (bindArgFinders(binding, param)) {
                continue;
            }

            throw missingNamedParameter(param.name, binding);
        }
    }

    protected <T> boolean bindArgFinders(
        List<T> nafs,
        Function<T, NamedArgumentFinder> getter,
        Param param)
    {
        for (T naf : nafs) {
            Optional<Argument> found = getter.apply(naf).find(param.name, ctx);
            if (found.isPresent()) {
                bindAndMaybeCache(__ -> applyArgument(found.get(), param), null);
                return true;
            }
        }
        return false;
    }

    protected boolean bindArgFinders(Binding binding, Param param) {
        return bindArgFinders(binding.namedArgumentFinder, v -> v, param);
    }

    private void bindNamedCheck(Binding binding) {
        // best effort: compare empty to non-empty because we can't list the individual binding names (unless we expose a method to do so)
        boolean argumentsProvidedButNoneDeclared = params.getParameterNames().isEmpty() && !binding.isEmpty();
        if (argumentsProvidedButNoneDeclared && !ctx.getConfig(SqlStatements.class).isUnusedBindingAllowed()) {
            throw new UnableToCreateStatementException(format(
                    "Superfluous named parameters provided while the query "
                            + "declares none: '%s'. This check may be disabled by calling "
                            + "getConfig(SqlStatements.class).setUnusedBindingAllowed(true) "
                            + "or using @AllowUnusedBindings in SQL object.", binding), ctx);
        }
    }

    @NonNull
    QualifiedType<?> typeOf(@Nullable Object value) {
        return value instanceof TypedValue
                ? ((TypedValue) value).getType()
                : ctx.getConfig(Qualifiers.class).qualifiedTypeOf(
                        Optional.ofNullable(value).<Class<?>>map(Object::getClass).orElse(Object.class));
    }

    /**
     * @deprecated prepare the argument by type instead
     */
    @Deprecated
    Argument toArgument(Object found) {
        return argumentFactoryForType(typeOf(found))
                .apply(unwrap(found));
    }

    private Function<Object, Argument> argumentFactoryForType(QualifiedType<?> type) {
        return argumentFactoryByType.computeIfAbsent(type, qt -> {
            Arguments args = ctx.getConfig(Arguments.class);
            Function<Object, Argument> factory =
                args.prepareFor(type)
                    .orElse(v -> args.findFor(type, v)
                            .orElseThrow(() -> factoryNotFound(type, v)));
            return value -> DescribedArgument.wrap(ctx, factory.apply(value), value);
        });
    }

    protected final UnableToCreateStatementException missingNamedParameter(String name, Binding binding) {
        return new UnableToCreateStatementException(format("Missing named parameter '%s' in binding:%s", name, binding), ctx);
    }

    private UnableToCreateStatementException factoryNotFound(QualifiedType<?> qualifiedType, Object value) {
        Type type = qualifiedType.getType();
        if (type instanceof Class<?>) { // not a ParameterizedType
            final TypeVariable<?>[] typeVars = ((Class<?>) type).getTypeParameters();
            if (typeVars.length > 0) {
                return new UnableToCreateStatementException("No type parameters found for erased type '" + type + Arrays.toString(typeVars)
                    + "' with qualifiers '" + qualifiedType.getQualifiers()
                    + "'. To bind a generic type, prefer using bindByType.");
            }
        }
        return new UnableToCreateStatementException("No argument factory registered for '" + value + "' of qualified type " + qualifiedType, ctx);
    }

    @CheckForNull
    static Object unwrap(@Nullable Object maybeTypedValue) {
        return maybeTypedValue instanceof TypedValue ? ((TypedValue) maybeTypedValue).getValue() : maybeTypedValue;
    }

    static class Prepared extends ArgumentBinder {
        private final PreparedBatch batch;
        private boolean useCache;
        private final List<Consumer<Binding>> cachedBinders = new ArrayList<>();
        private Object cacheKey;

        public Prepared(
            PreparedBatch batch,
            PreparedStatement stmt,
            StatementContext ctx,
            ParsedParameters params)
        {
            super(stmt, ctx, params);
            this.batch = batch;
            useCache = batch.size() > 1;
        }

        @Override
        protected boolean bindCached(Binding binding) {
            if (!useCache) {
                return false;
            }

            Object newCacheKey = ((PreparedBinding) binding).computeCacheKey(this::typeOf);
            if (cacheKey == null) {
                cacheKey = newCacheKey;
            } else if (!cacheKey.equals(newCacheKey)) {
                useCache = false;
                cachedBinders.clear();
            }
            if (cachedBinders.isEmpty()) {
                return false;
            }
            for (Consumer<Binding> binder: cachedBinders) {
                binder.accept(binding);
            }
            return true;
        }

        @Override
        protected void bindAndMaybeCache(Consumer<Binding> binder, Binding binding) {
            super.bindAndMaybeCache(binder, binding);
            if (useCache) {
                cachedBinders.add(binder);
            }
        }

        @Override
        protected boolean bindArgFinders(Binding binding, Param param) {
            PreparedBinding prepBinding = (PreparedBinding) binding;
            List<Supplier<NamedArgumentFinder>> backupNafs = Collections.emptyList();
            for (NamedArgumentFinderFactory.PrepareKey pk: prepBinding.prepareKeys.keySet()) {
                Function<Object, Argument> prep = batch.preparedFinders.get(pk)
                    .apply(param.name)
                    .orElse(null);
                if (prep == null) {
                    backupNafs = prepBinding.backupArgumentFinders;
                } else {
                    bindAndMaybeCache(b -> {
                        PreparedBinding prepB = (PreparedBinding) b;
                        Argument arg = prep.apply(prepB.prepareKeys.get(pk));
                        applyArgument(arg, param);
                    }, binding);

                    return true;
                }
            }
            return bindArgFinders(binding.namedArgumentFinder, v -> v, param) ||
                bindArgFinders(backupNafs, Supplier::get, param);
        }
    }
}
