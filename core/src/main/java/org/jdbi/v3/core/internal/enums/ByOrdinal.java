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
package org.jdbi.v3.core.internal.enums;

import java.sql.Types;
import java.util.Optional;

import org.jdbi.v3.core.EnumByOrdinal;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.result.UnableToProduceResultException;

@EnumByOrdinal
public class ByOrdinal implements EnumStrategy {
    private static final EnumStrategy INSTANCE = new ByOrdinal();

    public static EnumStrategy singleton() {
        return INSTANCE;
    }

    @Override
    public int nullType() {
        return Types.INTEGER;
    }

    @Override
    public Optional<Argument> bind(Arguments args, Enum<?> value) {
        return args.findFor(Integer.class, value.ordinal());
    }

    @Override
    public <E extends Enum<E>> ColumnMapper<E> getMapper(Class<E> enumClass) {
        final Enum<?>[] constants = enumClass.getEnumConstants();
        return (rs, col, ctx) -> {
            Integer ordinal = ctx.findColumnMapperFor(Integer.class)
                .orElseThrow(() -> new UnableToProduceResultException("an Integer column mapper is required to map Enums from ordinals", ctx))
                .map(rs, col, ctx);

            try {
                return ordinal == null ? null : enumClass.cast(constants[ordinal]);
            } catch (ArrayIndexOutOfBoundsException oob) {
                throw new UnableToProduceResultException(String.format(
                    "no %s value could be matched to the ordinal %s", enumClass.getSimpleName(), ordinal
                ), oob, ctx);
            }
        };
    }
}
