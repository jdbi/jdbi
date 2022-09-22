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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.inferred.freebuilder.shaded.com.google.common.base.Functions;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.argument.internal.NamedArgumentFinderFactory;
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
    final Map<String, Integer> paramToIndex;
    final Map<QualifiedType<?>, Function<Object, Argument>> argumentFactoryByType = new HashMap<>();

    ArgumentBinder(PreparedStatement stmt, StatementContext ctx, ParsedParameters params) {
        this.stmt = stmt;
        this.ctx = ctx;
        this.params = params;

        List<String> paramNames = params.getParameterNames();
        this.paramToIndex = params.isPositional()
            ? Collections.emptyMap()
            : IntStream.range(0, paramNames.size())
                .boxed()
                .collect(Collectors.toMap(paramNames::get, i -> i));
    }

    void bind(Binding binding) {
        if (params.isPositional()) {
            bindPositional(binding);
        } else {
            bindNamed(binding);
        }
    }

    private static final class ParamId {
        private final Object value;

        private ParamId(Object value) {
            this.value = value;
        }

        public static ParamId of(String value) {
            return new ParamId(value);
        }

        public static ParamId of(int value) {
            return new ParamId(value);
        }

        public Optional<String> getName() {
            return value.getClass() == String.class ? Optional.of((String) value) : Optional.empty();
        }

        public Optional<Integer> getIndex() {
            return value.getClass() == Integer.class ? Optional.of((Integer) value) : Optional.empty();
        }
    }

    private void applyArgument(Argument arg, ParamId paramId) {
        int index = paramId.getName().map(paramToIndex::get).orElseGet(() -> paramId.getIndex().get());
        try {
            arg.apply(index + 1, stmt, ctx);
        } catch (SQLException e) {
            String msg = paramId.getName().map(v -> format("Exception while binding named parameter '%s'", v))
                .orElseGet(() -> "Exception while binding positional param at (0 based) position " + index);
            throw new UnableToCreateStatementException(msg, e, ctx);
        }
    }

    private void bind(ParamId paramId, Object value) {
        Function<Object, Argument> binder = argumentFactoryForType(typeOf(value));
        Argument arg = binder.apply(unwrap(value));
        applyArgument(arg, paramId);
    }

    private void bindPositional(Binding binding) {
        boolean moreArgumentsProvidedThanDeclared = binding.positionals.size() != params.getParameterCount();
        if (moreArgumentsProvidedThanDeclared && !ctx.getConfig(SqlStatements.class).isUnusedBindingAllowed()) {
            throw new UnableToCreateStatementException("Superfluous positional param at (0 based) position " + params.getParameterCount(), ctx);
        }
        for (Map.Entry<Integer, Object> positional: binding.positionals.entrySet()) {
            bind(ParamId.of(positional.getKey()), Optional.of(positional.getKey()));
        }
    }

    private void bindNamed(Binding binding) {
        bindNamedCheck(binding);

        PreparedBinding prepBinding = binding instanceof PreparedBinding ? (PreparedBinding) binding : null;

        for (Map.Entry<String, Object> named: binding.named.entrySet()) {
            bind(ParamId.of(named.getKey()), named.getValue());
        }

        if (binding.named.size() < params.getParameterNames().size()) {
            outer:
            for (String paramName: params.getParameterNames()) {
                if (binding.named.containsKey(paramName)) {
                    continue;
                }
                List<Supplier<NamedArgumentFinder>> backupNafs = Collections.emptyList();
                if (prepBinding != null) {
                    for (Map.Entry<NamedArgumentFinderFactory.PrepareKey, Object> pkAndValue: prepBinding.prepareKeys.entrySet()) {
                        Function<Object, Argument> prep = prepBinding.batch.preparedFinders.get(pkAndValue.getKey())
                            .apply(paramName)
                            .orElse(null);
                        if (prep == null) {
                            backupNafs = prepBinding.backupArgumentFinders;
                        } else {
                            Argument arg = prep.apply(pkAndValue.getValue());
                            applyArgument(arg, ParamId.of(paramName));
                            continue outer;
                        }
                    }
                }
                if (bindArgFinders(binding.namedArgumentFinder, v -> v, paramName) ||
                    bindArgFinders(backupNafs, Supplier::get, paramName))
                {
                    continue;
                }
                throw missingNamedParameter(paramName, binding);
            }
        }
    }

    private <T> boolean bindArgFinders(
        List<T> nafs,
        Function<T, NamedArgumentFinder> getter,
        String paramName)
    {
        for (T naf : nafs) {
            Optional<Argument> found = getter.apply(naf).find(paramName, ctx);
            if (found.isPresent()) {
                applyArgument(found.get(), ParamId.of(paramName));
                return true;
            }
        }
        return false;
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
}
