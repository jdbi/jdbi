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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.NamedArgumentFinder.Prepared;
import org.jdbi.v3.core.argument.internal.TypedValue;
import org.jdbi.v3.core.internal.JdbiOptionals;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;

class ArgumentBinder {
    final StatementContext ctx;
    final ParsedParameters params;
    final List<Function<Binding, Argument>> preparedBinders;

    static ArgumentBinder of(StatementContext ctx, ParsedParameters parameters) {
        return new ArgumentBinder(ctx, parameters);
    }

    ArgumentBinder(StatementContext ctx, ParsedParameters params) {
        this.ctx = ctx;
        this.params = params;
        preparedBinders = new ArrayList<>(params.getParameterCount());
    }

    Argument toArgument(Object value) {
        return toArgument(typeOf(value), value);
    }

    Argument toArgument(QualifiedType<?> type, Object value) {
        return wrapArgumentToString(ctx.getConfig(Arguments.class).findFor(type, value)
                .orElseThrow(() -> factoryNotFound(type, value)), value);
    }

    Argument wrapArgumentToString(Argument arg, Object value) {
        try {
            boolean toStringIsImplementedInArgument = arg.getClass().getMethod("toString").getDeclaringClass() != Object.class;

            if (toStringIsImplementedInArgument) {
                return arg;
            } else {
                return new Argument() {
                    @Override
                    public void apply(int position, PreparedStatement statement, StatementContext ctx2) throws SQLException {
                        arg.apply(position, statement, ctx2);
                    }

                    @Override
                    public String toString() {
                        return Objects.toString(unwrap(value));
                    }
                };
            }
        } catch (NoSuchMethodException e) {
            throw new Error("toString method does not exist, Object hierarchy is corrupt", e);
        }
    }

    Function<Object, Argument> prepareBinderForType(QualifiedType<?> type) {
        final Function<Object, Argument> preparedBinder =
            ctx.getConfig(Arguments.class)
                .prepareFor(type)
                .orElse(v -> toArgument(type, v));
        return value -> wrapArgumentToString(preparedBinder.apply(value), value);
    }

    private UnsupportedOperationException factoryNotFound(QualifiedType<?> qualifiedType, Object value) {
        Type type = qualifiedType.getType();
        if (type instanceof Class<?>) { // not a ParameterizedType
            final TypeVariable<?>[] typeVars = ((Class<?>) type).getTypeParameters();
            if (typeVars.length > 0) {
                return new UnsupportedOperationException("No type parameters found for erased type '" + type + Arrays.toString(typeVars)
                    + "' with qualifiers '" + qualifiedType.getQualifiers()
                    + "'. To bind a generic type, prefer using bindByType.");
            }
        }
        return new UnsupportedOperationException("No argument factory registered for '" + value + "' of qualified type " + qualifiedType);
    }

    void bind(Binding binding, PreparedStatement statement, StatementContext context) {
        if (params.isPositional()) {
            bindPositional(binding, statement, context);
        } else {
            bindNamed(binding, statement, context);
        }
    }

    void bindPositional(Binding binding, PreparedStatement statement, StatementContext context) {
        boolean moreArgumentsProvidedThanDeclared = binding.positionals.size() != params.getParameterCount();
        if (moreArgumentsProvidedThanDeclared && !context.getConfig(SqlStatements.class).isUnusedBindingAllowed()) {
            throw new UnableToCreateStatementException("Superfluous positional param at (0 based) position " + params.getParameterCount(), context);
        }
        if (preparedBinders.isEmpty()) {
            for (int index = 0; index < params.getParameterCount(); index++) {
                QualifiedType<?> type = typeOf(binding.positionals.get(index));
                Function<Object, Argument> prepared = prepareBinderForType(type);
                final int i = index;
                preparedBinders.add(
                    b -> prepared.apply(unwrap(b.positionals.get(i))));
            }
        }
        for (int index = 0; index < params.getParameterCount(); index++) {
            try {
                preparedBinders.get(index)
                    .apply(binding)
                    .apply((int) index + 1, statement, context);
            } catch (SQLException e) {
                throw new UnableToCreateStatementException("Exception while binding positional param at (0 based) position " + index, e, context);
            }
        }
    }

    void bindNamed(Binding binding, PreparedStatement statement, StatementContext context) {
        List<String> paramNames = params.getParameterNames();
        // best effort: compare empty to non-empty because we can't list the individual binding names (unless we expose a method to do so)
        boolean argumentsProvidedButNoneDeclared = paramNames.isEmpty() && !binding.isEmpty();
        if (argumentsProvidedButNoneDeclared && !context.getConfig(SqlStatements.class).isUnusedBindingAllowed()) {
            throw new UnableToCreateStatementException(String.format(
                    "Superfluous named parameters provided while the query "
                            + "declares none: '%s'. This check may be disabled by calling "
                            + "getConfig(SqlStatements.class).setUnusedBindingAllowed(true) "
                            + "or using @AllowUnusedBindings in SQL object.", binding), context);
        }
        if (preparedBinders.isEmpty()) {
            for (int i = 0; i < paramNames.size(); i++) { //NOPMD
                final String name = paramNames.get(i);
                final Object value = binding.named.get(name);
                if (value == null && !binding.named.containsKey(name)) {
                    Optional<Prepared> prep =
                        binding.namedArgumentFinder.stream()
                            .flatMap(naf -> JdbiOptionals.stream(naf.prepare(name, context)))
                            .findFirst();
                    if (prep.isPresent()) {
                        Prepared p = prep.get();
                        Function<Object, Argument> binder = prepareBinderForType(p.type());
                        preparedBinders.add(b -> binder.apply(b.namedArgumentFinder.stream()
                                .flatMap(f -> JdbiOptionals.stream(f.unprepare(name, ctx).map(p.get())))
                                .findFirst()
                                .orElse(null)));
                    } else {
                        preparedBinders.add(b -> b.namedArgumentFinder.stream()
                            .flatMap(naf -> JdbiOptionals.stream(naf.find(name, context)))
                            .findFirst()
                            .orElseThrow(() -> missingNamedParameter(name, binding)));
                    }
                } else {
                    Function<Object, Argument> binder = prepareBinderForType(typeOf(value));
                    preparedBinders.add(b -> binder.apply(unwrap(b.named.get(name))));
                }
            }
        }

        for (int i = 0; i < preparedBinders.size(); i++) {
            try {
                preparedBinders.get(i)
                    .apply(binding)
                    .apply(i + 1, statement, context);
            } catch (SQLException e) {
                throw new UnableToCreateStatementException(
                        String.format("Exception while binding named parameter '%s'", paramNames.get(i)),
                        e, context);
            }
        }
    }

    UnableToCreateStatementException missingNamedParameter(String name, Binding binding) {
        return new UnableToCreateStatementException(String.format("Missing named parameter '%s' in binding:%s", name, binding), ctx);
    }

    QualifiedType<?> typeOf(Object value) {
        return value instanceof TypedValue
                ? ((TypedValue) value).getType()
                : ctx.getConfig(Qualifiers.class).qualifiedTypeOf(
                        Optional.ofNullable(value).<Class<?>>map(Object::getClass).orElse(Object.class));
    }

    static Object unwrap(Object maybeTypedValue) {
        return maybeTypedValue instanceof TypedValue ? ((TypedValue) maybeTypedValue).getValue() : maybeTypedValue;
    }

    interface BindingStrategy {
        void bind(Binding binding, PreparedStatement statement, StatementContext context);
    }
}
