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
package org.jdbi.v3.core.mapper;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.JdbiOptionals;

/**
 * @deprecated will be replaced by an opt-out plugin to give the core no hardwired behavior
 */
@Deprecated
// TODO remove this entire class
public class BuiltInMapperFactory implements ColumnMapperFactory {
    private static final List<ColumnMapperFactory> FACTORIES = Arrays.asList(
        new EnumByNameMapperFactory(),
        new OptionalMapperFactory(),
        new PrimitiveMapperFactory(),
        new BoxedMapperFactory(),
        new EssentialsMapperFactory(),
        new InternetMapperFactory(),
        new SqlTimeMapperFactory(),
        new JavaTimeMapperFactory()
    );

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        return FACTORIES.stream()
            .flatMap(factory -> JdbiOptionals.stream(factory.build(type, config)))
            .findFirst();
    }
}
