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
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.JdbiOptionals;
import org.jdbi.v3.core.internal.QualifiedEnumArgumentFactory;
import org.jdbi.v3.core.statement.SqlStatement;

/**
 * The BuiltInArgumentFactory provides instances of {@link Argument} for
 * many core Java types.  Generally you should not need to use this
 * class directly, but instead should bind your object with the
 * {@link SqlStatement} convenience methods.
 *
 * @deprecated will be replaced by a plugin
 */
@Deprecated
public class BuiltInArgumentFactory implements ArgumentFactory {
    public static final ArgumentFactory INSTANCE = new BuiltInArgumentFactory();

    private static final List<ArgumentFactory> FACTORIES = Arrays.asList(
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
    public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
        return FACTORIES.stream()
            .flatMap(factory -> JdbiOptionals.stream(factory.build(expectedType, value, config)))
            .findFirst();
    }

    // TODO untyped null bug
    private static class LegacyEnumByNameArgumentFactory implements ArgumentFactory {
        @Override
        public Optional<Argument> build(Type expectedType, Object rawValue, ConfigRegistry config) {
            return rawValue instanceof Enum
                ? QualifiedEnumArgumentFactory.byName().build(expectedType, rawValue, config)
                : Optional.empty();
        }
    }
}
