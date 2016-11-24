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

import static org.jdbi.v3.core.util.GenericTypes.getErasedType;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.sql.Types;
import java.util.Optional;

import org.jdbi.v3.core.ConfigRegistry;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;

/**
 * Postgres version of argument factory for {@code InetAddress}.
 */
public class InetArgumentFactory implements ArgumentFactory {
    @Override
    public Optional<Argument> build(Type type, ConfigRegistry config) {
        if (InetAddress.class.isAssignableFrom(getErasedType(type))) {
            return Optional.of((p, i, value, cx) -> p.setObject(i, ((InetAddress) value).getHostAddress(), Types.OTHER));
        }
        return Optional.empty();
    }
}
