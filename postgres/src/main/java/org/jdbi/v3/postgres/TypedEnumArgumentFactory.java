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

import java.sql.Types;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * Default {@code jdbi} behavior is to bind {@code Enum} subclasses as
 * a string, which Postgres won't implicitly convert to an enum type.
 * If instead you bind it as {@code java.sql.Types.OTHER}, Postgres will
 * autodetect the enum correctly.
 */
public class TypedEnumArgumentFactory extends AbstractArgumentFactory<Enum<?>> {
    public TypedEnumArgumentFactory() {
        super(Types.OTHER);
    }

    @Override
    protected Argument build(Enum<?> value, ConfigRegistry config) {
        return (p, s, c) -> s.setObject(p, value, Types.OTHER);
    }
}
