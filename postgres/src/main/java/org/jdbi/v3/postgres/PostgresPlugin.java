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
    public void customizeJdbi(Jdbi db) {
        db.registerArgument(new TypedEnumArgumentFactory());
        db.registerArgument(new JavaTimeArgumentFactory());
        db.registerArgument(new InetArgumentFactory());
        db.registerArgument(new HStoreArgumentFactory());

        db.registerArrayType(int.class, "integer");
        db.registerArrayType(Integer.class, "integer");
        db.registerArrayType(long.class, "bigint");
        db.registerArrayType(Long.class, "bigint");
        db.registerArrayType(String.class, "varchar");
        db.registerArrayType(UUID.class, "uuid");
        db.registerArrayType(float.class, "real");
        db.registerArrayType(Float.class, "real");
        db.registerArrayType(double.class, "double precision");
        db.registerArrayType(Double.class, "double precision");

        db.registerColumnMapper(new JavaTimeMapperFactory());
        db.registerColumnMapper(new HStoreColumnMapper());
    }
}
