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
package org.jdbi.v3.core.argument;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.enums.EnumByName;
import org.jdbi.v3.core.internal.JdbiOptionals;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

/**
 * The BuiltInArgumentFactory provides instances of {@link Argument} for
 * many core Java types.  Generally you should not need to use this
 * class directly, but instead should bind your object with the
 * {@link SqlStatement} convenience methods.
 *
 * @deprecated will be replaced by a plugin
 */
@Deprecated
public class BuiltInArgumentFactory implements ArgumentFactory.Preparable {
    public static final ArgumentFactory INSTANCE = new BuiltInArgumentFactory();

    private static final List<ArgumentFactory.Preparable> FACTORIES = Arrays.asList(
        new PrimitivesArgumentFactory(),
        new BoxedArgumentFactory(),
        new EssentialsArgumentFactory(),
        new SqlArgumentFactory(),
        new InternetArgumentFactory(),
        new SqlTimeArgumentFactory(),
        new JavaTimeArgumentFactory(),
        new LegacyEnumByNameArgumentFactory(),
        new OptionalArgumentFactory(),
        new UntypedNullArgumentFactory()
    );

    @Override
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config) {
        return FACTORIES.stream()
            .flatMap(factory -> JdbiOptionals.stream(factory.prepare(type, config)))
            .findFirst();
    }

    @Override
    public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
        return FACTORIES.stream()
            .flatMap(factory -> JdbiOptionals.stream(factory.build(expectedType, value, config)))
            .findFirst();
    }

    @Override
    public Collection<? extends Type> prePreparedTypes() {
        return FACTORIES.stream()
                .map(ArgumentFactory.Preparable::prePreparedTypes)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static class LegacyEnumByNameArgumentFactory implements ArgumentFactory.Preparable {
        private final EnumArgumentFactory delegate = new EnumArgumentFactory();
        @Override
        public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config) {
            return EnumArgumentFactory.ifEnum(type).map(clazz ->
                    value -> build(type, value, config)
                        .orElseThrow(() -> new UnableToCreateStatementException("No enum value to bind after prepare")));
        }
        @Override
        public Optional<Argument> build(Type expectedType, Object rawValue, ConfigRegistry config) {
            return delegate.build(QualifiedType.of(expectedType).with(EnumByName.class), rawValue, config);
        }
    }
}
