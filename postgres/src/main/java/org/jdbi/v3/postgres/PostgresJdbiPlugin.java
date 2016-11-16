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

import org.jdbi.v3.core.ArgumentRegistry;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.MappingRegistry;
import org.jdbi.v3.core.spi.JdbiPlugin;

public class PostgresJdbiPlugin implements JdbiPlugin {
    @Override
    public void customizeJdbi(Jdbi db) {
        db.getConfig(ArgumentRegistry.class)
                .register(new TypedEnumArgumentFactory())
                .register(new JavaTimeArgumentFactory())
                .register(new InetArgumentFactory())
                .register(new HStoreArgumentFactory())
                .registerArrayType(int.class, "integer")
                .registerArrayType(Integer.class, "integer")
                .registerArrayType(long.class, "bigint")
                .registerArrayType(Long.class, "bigint")
                .registerArrayType(String.class, "varchar")
                .registerArrayType(UUID.class, "uuid")
                .registerArrayType(float.class, "real")
                .registerArrayType(Float.class, "real")
                .registerArrayType(double.class, "double precision")
                .registerArrayType(Double.class, "double precision");

        db.getConfig(MappingRegistry.class)
                .registerColumnMapper(new JavaTimeMapperFactory())
                .registerColumnMapper(new HStoreColumnMapper());
    }
}
