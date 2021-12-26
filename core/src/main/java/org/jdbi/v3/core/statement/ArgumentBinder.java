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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
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

import static java.lang.String.format;

class ArgumentBinder<Stmt extends SqlStatement<?>> {
    final PreparedStatement stmt;
    final StatementContext ctx;
    final ParsedParameters params;
    final Map<QualifiedType<?>, Function<Object, Argument>> argumentFactoryByType = new HashMap<>();

    private final Argument nullArgument;

    ArgumentBinder(PreparedStatement stmt, StatementContext ctx, ParsedParameters params) {
        this.stmt = stmt;
        this.ctx = ctx;
        this.params = params;

        this.nullArgument = ctx.getConfig(Arguments.class).getUntypedNullArgument();
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
                    if (value instanceof Argument) {
                        ((Argument) value).apply(i + 1, stmt, ctx);
                    } else {
                        // value set, find an argument factory and assign the value
                        argumentFactoryForType(typeOf(value))
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

    static class Prepared extends ArgumentBinder<PreparedBatch> {
        private final PreparedBatch batch;
        private final Consumer<PreparedBinding> preparedBinder;
        private final List<String> paramNames;

        private final Argument nullArgument;

        Prepared(PreparedBatch batch, ParsedParameters params, PreparedBinding preparedBindingTemplate) {
            super(batch.stmt, batch.getContext(), params);
            this.batch = batch;
            this.paramNames = params.getParameterNames();
            this.nullArgument = batch.getContext().getConfig(Arguments.class).getUntypedNullArgument();

            this.preparedBinder = prepareBinder(preparedBindingTemplate);
        }

        private Consumer<PreparedBinding> prepareBinder(PreparedBinding preparedBinding) {
            List<Consumer<PreparedBinding>> innerBinders = new ArrayList<>(paramNames.size());
            for (int i = 0; i < paramNames.size(); i++) {
                final int index = i;
                final String name = paramNames.get(i);
                final Object value = preparedBinding.named.get(name);
                if (value == null) {
                    if (preparedBinding.named.containsKey(name)) {
                        // name is present and value is null. Bind a null.
                        innerBinders.add(wrapCheckedConsumer(name, binding -> nullArgument.apply(index + 1, stmt, ctx)));
                    } else {
                        final Optional<Entry<PrepareKey, Function<Object, Argument>>> preparation =
                            preparedBinding.prepareKeys.keySet().stream()
                                .map(pk -> new AbstractMap.SimpleImmutableEntry<>(pk, batch.preparedFinders.get(pk)))
                                .flatMap(e ->
                                    JdbiOptionals.stream(e.getValue()
                                        .apply(name)
                                        .<Entry<PrepareKey, Function<Object, Argument>>>map(pf -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), pf))))
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
                                    .flatMap(naf -> JdbiOptionals.stream(naf.find(name, ctx)))
                                    .findFirst()
                                    .orElseGet(() ->
                                        binding.realizedBackupArgumentFinders.get().stream()
                                            .flatMap(naf -> JdbiOptionals.stream(naf.find(name, ctx)))
                                            .findFirst()
                                            .orElseThrow(() -> missingNamedParameter(name, binding)))
                                    .apply(index + 1, stmt, ctx)));
                        }
                    }
                } else {
                    if (value instanceof Argument) {
                        // if the value is already an argument, skip the machinery and bind the value
                        // directly in the right position
                        innerBinders.add(wrapCheckedConsumer(name, binding -> ((Argument) value).apply(index + 1, stmt, ctx)));
                    } else {
                        // otherwise find a function that translates the value to an argument and
                        // bind it as an inner binder
                        final Function<Object, Argument> binder = argumentFactoryForType(typeOf(value));

                        innerBinders.add(wrapCheckedConsumer(name,
                            binding -> binder.apply(unwrap(binding.named.get(name)))
                                .apply(index + 1, stmt, ctx)));
                    }
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
