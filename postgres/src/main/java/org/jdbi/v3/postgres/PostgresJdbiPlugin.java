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

public class PostgresJdbiPlugin implements JdbiPlugin {
    @Override
    public void customizeDbi(Jdbi db) {
        db.registerArgumentFactory(new TypedEnumArgumentFactory());

        db.registerColumnMapper(new JavaTimeMapperFactory());
        db.registerArgumentFactory(new JavaTimeArgumentFactory());

        db.registerArgumentFactory(new InetArgumentFactory());

        db.registerColumnMapper(new HStoreColumnMapper());
        db.registerArgumentFactory(new HStoreArgumentFactory());

        db.registerArrayElementTypeName(int.class, "integer");
        db.registerArrayElementTypeName(Integer.class, "integer");
        db.registerArrayElementTypeName(long.class, "bigint");
        db.registerArrayElementTypeName(Long.class, "bigint");
        db.registerArrayElementTypeName(String.class, "varchar");
        db.registerArrayElementTypeName(UUID.class, "uuid");
        db.registerArrayElementTypeName(float.class, "real");
        db.registerArrayElementTypeName(Float.class, "real");
        db.registerArrayElementTypeName(double.class, "double precision");
        db.registerArrayElementTypeName(Double.class, "double precision");
    }
}
