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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.argument.internal.NamedArgumentFinderFactory.PrepareKey;
import org.jdbi.v3.core.argument.internal.TypedValue;
import org.jdbi.v3.core.internal.exceptions.CheckedConsumer;
import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;
import org.jdbi.v3.core.statement.internal.PreparedBinding;

import static java.lang.String.format;

class ArgumentBinder {
    final PreparedStatement stmt;
    final StatementContext ctx;
    final ParsedParameters params;

    private final Argument nullArgument;
    protected final ArgumentFactoryLocator factoryLocator;

    ArgumentBinder(PreparedStatement stmt, StatementContext ctx, ParsedParameters params) {
        this.stmt = stmt;
        this.ctx = ctx;
        this.params = params;

        this.nullArgument = ctx.getConfig(Arguments.class).getUntypedNullArgument();
        this.factoryLocator = new ArgumentFactoryLocator(ctx);
    }

    void bind(Binding binding) {
        if (params.isPositional()) {
            bindPositional(binding);
        } else {
            bindNamed(binding);
        }
    }

    void bindPositional(Binding binding) {
        for (int index = 0; index < params.getParameterCount(); index++) {
            if (!binding.positionals.containsKey(index)) {
                throw new UnableToCreateStatementException(format("Missing positional parameter %d in binding:%s", index, binding), ctx);
            }
            QualifiedType<?> type = factoryLocator.typeOf(binding.positionals.get(index));
            try {
                factoryLocator.argumentFactoryForType(type)
                    .apply(unwrap(binding.positionals.get(index)))
                    .apply(index + 1, stmt, ctx);
            } catch (SQLException e) {
                throw new UnableToCreateStatementException("Exception while binding positional param at (0 based) position " + index, e, ctx);
            }
        }
        boolean moreArgumentsProvidedThanDeclared = binding.positionals.size() != params.getParameterCount();
        if (moreArgumentsProvidedThanDeclared && !ctx.getConfig(SqlStatements.class).isUnusedBindingAllowed()) {
            throw new UnableToCreateStatementException("Superfluous positional param at (0 based) position " + params.getParameterCount(), ctx);
        }
    }

    void bindNamed(Binding binding) {
        final List<String> paramNames = params.getParameterNames();
        bindNamedCheck(binding, paramNames);

        assignNames:
        for (int i = 0; i < paramNames.size(); i++) {
            final String name = paramNames.get(i);
            try {
                final Object value = binding.named.get(name);
                if (value == null) {
                    if (binding.named.containsKey(name)) {
                        // bind a null for the given name
                        nullArgument.apply(i + 1, stmt, ctx);
                    } else {
                        // binding was not set, look through the named argument finders
                        for (NamedArgumentFinder naf : binding.namedArgumentFinder) {
                            Optional<Argument> found = naf.find(name, ctx);
                            if (found.isPresent()) {
                                found.get().apply(i + 1, stmt, ctx);
                                continue assignNames;
                            }
                        }
                        throw missingNamedParameter(name, binding);
                    }
                } else {
                    if (value instanceof Argument argument) {
                        argument.apply(i + 1, stmt, ctx);
                    } else {
                        // value set, find an argument factory and assign the value
                        factoryLocator.argumentFactoryForType(factoryLocator.typeOf(value))
                            .apply(unwrap(value))
                            .apply(i + 1, stmt, ctx);
                    }
                }
            } catch (SQLException e) {
                throw new UnableToCreateStatementException(format("Exception while binding named parameter '%s'", name), e, ctx);
            }
        }
    }

    void bindNamedCheck(Binding binding, List<String> paramNames) {
        // best effort: compare empty to non-empty because we can't list the individual binding names (unless we expose a method to do so)
        boolean argumentsProvidedButNoneDeclared = paramNames.isEmpty() && !binding.isEmpty();
        if (argumentsProvidedButNoneDeclared && !ctx.getConfig(SqlStatements.class).isUnusedBindingAllowed()) {
            throw new UnableToCreateStatementException(format(
                    "Superfluous named parameters provided while the query "
                            + "declares none: '%s'. This check may be disabled by calling "
                            + "getConfig(SqlStatements.class).setUnusedBindingAllowed(true) "
                            + "or using @AllowUnusedBindings in SQL object.", binding), ctx);
        }
    }

    /**
     * @deprecated prepare the argument by type instead
     */
    @Deprecated(since = "3.11.0", forRemoval = true)
    Argument toArgument(Object found) {
        return factoryLocator.argumentFactoryForType(factoryLocator.typeOf(found))
                .apply(unwrap(found));
    }

    UnableToCreateStatementException missingNamedParameter(String name, Binding binding) {
        return new UnableToCreateStatementException(format("Missing named parameter '%s' in binding:%s", name, binding), ctx);
    }

    <T> Consumer<T> wrapCheckedConsumer(final String paramName, CheckedConsumer<T> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (SQLException e) {
                throw new UnableToCreateStatementException(
                        format("Exception while binding named parameter '%s'", paramName),
                        e, ctx);
            } catch (Exception e) {
                throw Sneaky.throwAnyway(e);
            }
        };
    }

    @CheckReturnValue
    static Object unwrap(@Nullable Object maybeTypedValue) {
        return maybeTypedValue instanceof TypedValue t ? t.getValue() : maybeTypedValue;
    }

    static class ArgumentFactoryLocator {
        private final StatementContext ctx;

        final Map<QualifiedType<?>, Function<Object, Argument>> argumentFactoryByType = new HashMap<>();

        ArgumentFactoryLocator(StatementContext ctx) {
            this.ctx = ctx;
        }

        @Nonnull
        QualifiedType<?> typeOf(@Nullable Object value) {
            return (value instanceof TypedValue t)
                ? t.getType()
                : ctx.getConfig(Qualifiers.class).qualifiedTypeOf(
                    Optional.ofNullable(value).<Class<?>>map(Object::getClass).orElse(Object.class));
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

        private UnableToCreateStatementException factoryNotFound(QualifiedType<?> qualifiedType, Object value) {
            Type type = qualifiedType.getType();
            if (type instanceof Class<?> clazz) { // not a ParameterizedType
                final TypeVariable<?>[] typeVars = clazz.getTypeParameters();
                if (typeVars.length > 0) {
                    return new UnableToCreateStatementException("No type parameters found for erased type '" + type + Arrays.toString(typeVars)
                        + "' with qualifiers '" + qualifiedType.getQualifiers()
                        + "'. To bind a generic type, prefer using bindByType.");
                }
            }
            return new UnableToCreateStatementException("No argument factory registered for '" + value + "' of qualified type " + qualifiedType, ctx);
        }
    }

    static class Prepared extends ArgumentBinder {
        private final PreparedBatch batch;
        private final Consumer<PreparedBinding> preparedBinder;
        private final List<String> paramNames;

        Prepared(PreparedBatch batch, ParsedParameters params, PreparedBinding preparedBindingTemplate) {
            super(batch.stmt, batch.getContext(), params);
            this.batch = batch;
            this.paramNames = params.getParameterNames();

            this.preparedBinder = prepareBinder(preparedBindingTemplate);
        }

        private Consumer<PreparedBinding> prepareBinder(PreparedBinding preparedBindingTemplate) {
            List<Consumer<PreparedBinding>> innerBinders = new ArrayList<>(paramNames.size());
            for (int i = 0; i < paramNames.size(); i++) {
                final int index = i;
                final String name = paramNames.get(i);
                final Object value = preparedBindingTemplate.named.get(name);
                if (value == null && !preparedBindingTemplate.named.containsKey(name)) {
                    final Optional<Entry<PrepareKey, Function<Object, Argument>>> preparation =
                        preparedBindingTemplate.prepareKeys.keySet().stream()
                            .map(pk -> new AbstractMap.SimpleImmutableEntry<>(pk, batch.preparedFinders.get(pk)))
                            .flatMap(e ->
                                e.getValue()
                                    .apply(name)
                                    .<Entry<PrepareKey, Function<Object, Argument>>>map(pf -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), pf)).stream())
                            .findFirst();
                    if (preparation.isPresent()) {
                        Entry<PrepareKey, Function<Object, Argument>> p = preparation.get();
                        innerBinders.add(wrapCheckedConsumer(name,
                            binding -> p.getValue()
                                .apply(binding.prepareKeys.get(p.getKey()))
                                .apply(index + 1, stmt, ctx)));
                    } else {
                        innerBinders.add(wrapCheckedConsumer(name,
                            binding -> binding.namedArgumentFinder.stream()
                                .flatMap(naf -> naf.find(name, ctx).stream())
                                .findFirst()
                                .orElseGet(() ->
                                    binding.realizedBackupArgumentFinders.get().stream()
                                        .flatMap(naf -> naf.find(name, ctx).stream())
                                        .findFirst()
                                        .orElseThrow(() -> missingNamedParameter(name, binding)))
                                .apply(index + 1, stmt, ctx)));
                    }
                } else {
                    final Function<Object, Argument> binder = factoryLocator.argumentFactoryForType(factoryLocator.typeOf(value));
                    innerBinders.add(wrapCheckedConsumer(name,
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
