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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.argument.internal.NamedArgumentFinderFactory.PrepareKey;
import org.jdbi.v3.core.argument.internal.TypedValue;
import org.jdbi.v3.core.internal.JdbiOptionals;
import org.jdbi.v3.core.internal.exceptions.CheckedConsumer;
import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.statement.internal.PreparedBinding;

class ArgumentBinder<Stmt extends SqlStatement<?>> {
    final PreparedStatement stmt;
    final StatementContext ctx;
    final ParsedParameters params;
    final Map<QualifiedType<?>, Function<Object, Argument>> argumentFactoryByType = new HashMap<>();

    ArgumentBinder(PreparedStatement stmt, StatementContext ctx, ParsedParameters params) {
        this.stmt = stmt;
        this.ctx = ctx;
        this.params = params;
    }

    void bind(Binding binding) {
        if (params.isPositional()) {
            bindPositional(binding);
        } else {
            bindNamed(binding);
        }
    }

    void bindPositional(Binding binding) {
        boolean moreArgumentsProvidedThanDeclared = binding.positionals.size() != params.getParameterCount();
        if (moreArgumentsProvidedThanDeclared && !ctx.getConfig(SqlStatements.class).isUnusedBindingAllowed()) {
            throw new UnableToCreateStatementException("Superfluous positional param at (0 based) position " + params.getParameterCount(), ctx);
        }
        for (int index = 0; index < params.getParameterCount(); index++) {
            QualifiedType<?> type = typeOf(binding.positionals.get(index));
            try {
                argumentFactoryForType(type)
                    .apply(unwrap(binding.positionals.get(index)))
                    .apply((int) index + 1, stmt, ctx);
            } catch (SQLException e) {
                throw new UnableToCreateStatementException("Exception while binding positional param at (0 based) position " + index, e, ctx);
            }
        }
    }

    void bindNamed(Binding binding) {
        final List<String> paramNames = params.getParameterNames();
        bindNamedCheck(binding, paramNames);
        for (int i = 0; i < paramNames.size(); i++) { // NOPMD
            final int index = i;
            wrapExceptions(() -> paramNames.get(index), x -> {
                final String name = paramNames.get(index);
                final Object value = binding.named.get(name);
                if (value == null && !binding.named.containsKey(name)) {
                    for (NamedArgumentFinder naf : binding.namedArgumentFinder) {
                        Optional<Argument> found = naf.find(name, ctx);
                        if (found.isPresent()) {
                            found.get().apply(index + 1, stmt, ctx);
                            return;
                        }
                    }
                    throw missingNamedParameter(name, binding);
                } else {
                    argumentFactoryForType(typeOf(value))
                            .apply(unwrap(value))
                            .apply(index + 1, stmt, ctx);
                }
            }).accept(null);
        }
    }

    void bindNamedCheck(Binding binding, List<String> paramNames) {
        // best effort: compare empty to non-empty because we can't list the individual binding names (unless we expose a method to do so)
        boolean argumentsProvidedButNoneDeclared = paramNames.isEmpty() && !binding.isEmpty();
        if (argumentsProvidedButNoneDeclared && !ctx.getConfig(SqlStatements.class).isUnusedBindingAllowed()) {
            throw new UnableToCreateStatementException(String.format(
                    "Superfluous named parameters provided while the query "
                            + "declares none: '%s'. This check may be disabled by calling "
                            + "getConfig(SqlStatements.class).setUnusedBindingAllowed(true) "
                            + "or using @AllowUnusedBindings in SQL object.", binding), ctx);
        }
    }

    QualifiedType<?> typeOf(Object value) {
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

    Function<Object, Argument> argumentFactoryForType(QualifiedType<?> type) {
        return argumentFactoryByType.computeIfAbsent(type, qt -> {
            Arguments args = ctx.getConfig(Arguments.class);
            Function<Object, Argument> factory =
                args.prepareFor(type)
                    .orElse(v -> args.findFor(type, v)
                            .orElseThrow(() -> factoryNotFound(type, v)));
            return value -> DescribedArgument.wrap(ctx, factory.apply(value), value);
        });
    }

    UnableToCreateStatementException missingNamedParameter(String name, Binding binding) {
        return new UnableToCreateStatementException(String.format("Missing named parameter '%s' in binding:%s", name, binding), ctx);
    }

    <T> Consumer<T> wrapExceptions(Supplier<String> paramName, CheckedConsumer<T> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (SQLException e) {
                throw new UnableToCreateStatementException(
                        String.format("Exception while binding named parameter '%s'", paramName.get()),
                        e, ctx);
            } catch (Exception e) {
                throw Sneaky.throwAnyway(e);
            }
        };
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

    static Object unwrap(Object maybeTypedValue) {
        return maybeTypedValue instanceof TypedValue ? ((TypedValue) maybeTypedValue).getValue() : maybeTypedValue;
    }

    static class Prepared extends ArgumentBinder<PreparedBatch> {
        final PreparedBatch batch;
        final Consumer<PreparedBinding> preparedBinder;
        final List<String> paramNames;

        Prepared(PreparedBatch batch, ParsedParameters params, PreparedBinding example) {
            super(batch.stmt, batch.getContext(), params);
            this.batch = batch;
            paramNames = params.getParameterNames();
            preparedBinder = prepareBinder(example);
        }

        private Consumer<PreparedBinding> prepareBinder(PreparedBinding example) {
            List<Consumer<PreparedBinding>> innerBinders = new ArrayList<>(paramNames.size());
            for (int i = 0; i < paramNames.size(); i++) { // NOPMD
                final int index = i;
                final String name = paramNames.get(i);
                final Object value = example.named.get(name);
                if (value == null && !example.named.containsKey(name)) {
                    final Optional<Entry<PrepareKey, Function<Object, Argument>>> preparation =
                        example.prepareKeys.keySet().stream()
                            .map(pk -> new AbstractMap.SimpleImmutableEntry<>(pk, batch.preparedFinders.get(pk)))
                            .flatMap(e ->
                                JdbiOptionals.stream(e.getValue()
                                    .apply(name)
                                    .<Entry<PrepareKey, Function<Object, Argument>>>map(pf -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), pf))))
                            .findFirst();
                    if (preparation.isPresent()) {
                        Entry<PrepareKey, Function<Object, Argument>> p = preparation.get();
                        innerBinders.add(wrapExceptions(
                                () -> name,
                                binding -> p.getValue()
                                    .apply(binding.prepareKeys.get(p.getKey()))
                                    .apply(index + 1, stmt, ctx)));
                    } else {
                        innerBinders.add(wrapExceptions(() -> name,
                                binding -> binding.namedArgumentFinder.stream()
                                    .flatMap(naf -> JdbiOptionals.stream(naf.find(name, ctx)))
                                    .findFirst()
                                    .orElseGet(() ->
                                        binding.realizedBackupArgumentFinders.get().stream()
                                            .flatMap(naf -> JdbiOptionals.stream(naf.find(name, ctx)))
                                            .findFirst()
                                            .orElseThrow(() -> missingNamedParameter(name, binding)))
                                    .apply(index + 1, stmt, ctx)));
                    }
                } else {
                    Function<Object, Argument> binder = argumentFactoryForType(typeOf(value));
                    innerBinders.add(wrapExceptions(() -> name,
                            binding -> binder.apply(unwrap(binding.named.get(name)))
                                .apply(index + 1, stmt, ctx)));
                }
            }
            return binding -> innerBinders.forEach(b -> b.accept(binding));
        }

        @Override
        void bindNamed(Binding binding) {
            bindNamedCheck(binding, paramNames);
            preparedBinder.accept((PreparedBinding) binding);
        }
    }
}
