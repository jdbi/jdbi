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
package org.jdbi.core.argument;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.enums.EnumByName;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.statement.SqlStatement;
import org.jdbi.core.statement.UnableToCreateStatementException;

/**
 * The BuiltInArgumentFactory provides instances of {@link Argument} for
 * many core Java types.  Generally you should not need to use this
 * class directly, but instead should bind your object with the
 * {@link SqlStatement} convenience methods.
 *
 * @deprecated will be replaced by a plugin
 */
@Deprecated(since = "3.6.0")
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
            .flatMap(factory -> factory.prepare(type, config).stream())
            .findFirst();
    }

    @Override
    public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
        return FACTORIES.stream()
            .flatMap(factory -> factory.build(expectedType, value, config).stream())
            .findFirst();
    }

    /**
     * @deprecated no longer used
     */
    @Override
    @Deprecated(since = "3.39.0", forRemoval = true)
    public Collection<? extends Type> prePreparedTypes() {
        return FACTORIES.stream()
                .map(ArgumentFactory.Preparable::prePreparedTypes)
                .flatMap(Collection::stream)
                .toList();
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
