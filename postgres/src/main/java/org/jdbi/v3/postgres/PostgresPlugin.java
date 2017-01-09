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

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;

public class PostgresPlugin implements JdbiPlugin {
    @Override
    public void customizeJdbi(Jdbi jdbi) {
        jdbi.registerArgument(new TypedEnumArgumentFactory());
        jdbi.registerArgument(new JavaTimeArgumentFactory());
        jdbi.registerArgument(new InetArgumentFactory());
        jdbi.registerArgument(new HStoreArgumentFactory());

        jdbi.registerArrayType(int.class, "integer");
        jdbi.registerArrayType(Integer.class, "integer");
        jdbi.registerArrayType(long.class, "bigint");
        jdbi.registerArrayType(Long.class, "bigint");
        jdbi.registerArrayType(String.class, "varchar");
        jdbi.registerArrayType(UUID.class, "uuid");
        jdbi.registerArrayType(float.class, "real");
        jdbi.registerArrayType(Float.class, "real");
        jdbi.registerArrayType(double.class, "double precision");
        jdbi.registerArrayType(Double.class, "double precision");

        jdbi.registerColumnMapper(new JavaTimeMapperFactory());
        jdbi.registerColumnMapper(new HStoreColumnMapper());
    }
}
