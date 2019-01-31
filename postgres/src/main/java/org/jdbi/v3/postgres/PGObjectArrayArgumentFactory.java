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

import java.sql.Array;
import java.sql.Types;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.internal.strategies.LoggableBinderArgument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.postgresql.PGConnection;
import org.postgresql.util.PGobject;

/**
 * Argument factory for array of {@link PGobject}.
 */
public class PGObjectArrayArgumentFactory extends AbstractArgumentFactory<PGobject[]> {

    public PGObjectArrayArgumentFactory() {
        super(Types.ARRAY);
    }

    @Override
    protected Argument build(PGobject[] value, ConfigRegistry config) {
        return new LoggableBinderArgument<>(value, (statement, index, val) -> {
            PGConnection pgConnection = (PGConnection) statement.getConnection();
            @SuppressWarnings("unchecked")
            Class<? extends PGobject> componentType = (Class<? extends PGobject>) val.getClass().getComponentType();
            String type = PostgresTypes.getTypeName(componentType);

            pgConnection.addDataType(type, componentType);
            Array array = statement.getConnection().createArrayOf(type, val);
            statement.setArray(index, array);
        });
    }

}
