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
package org.jdbi.v3.postgres;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.generic.GenericTypes;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

/**
 * An argument factory which binds Java's {@link Map} to Postgres' hstore type.
 */
public class HStoreArgumentFactory implements ArgumentFactory {

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        if (Map.class.isAssignableFrom(GenericTypes.getErasedType(type))) {
            return Optional.of((i, p, cx) -> p.setObject(i, value));
        }
        return Optional.empty();
    }
}
