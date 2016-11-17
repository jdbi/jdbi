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

import java.util.UUID;

import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.array.SqlArrayTypes;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.spi.JdbiPlugin;

public class PostgresJdbiPlugin implements JdbiPlugin {
    @Override
    public void customizeJdbi(Jdbi jdbi) {
        jdbi.getConfig(Arguments.class)
                .register(new TypedEnumArgumentFactory())
                .register(new JavaTimeArgumentFactory())
                .register(new InetArgumentFactory())
                .register(new HStoreArgumentFactory());

        jdbi.getConfig(SqlArrayTypes.class)
                .register(int.class, "integer")
                .register(Integer.class, "integer")
                .register(long.class, "bigint")
                .register(Long.class, "bigint")
                .register(String.class, "varchar")
                .register(UUID.class, "uuid")
                .register(float.class, "real")
                .register(Float.class, "real")
                .register(double.class, "double precision")
                .register(Double.class, "double precision");

        jdbi.getConfig(ColumnMappers.class)
                .register(new JavaTimeMapperFactory())
                .register(new HStoreColumnMapper());
    }
}
