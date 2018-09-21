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
package org.jdbi.v3.core.argument;

import java.sql.Types;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.qualifier.NVarchar;
import org.jdbi.v3.meta.Beta;

/**
 * Argument factory for {@code @NVarchar String} qualified type.
 */
@Beta
@NVarchar
class NVarcharArgumentFactory extends AbstractArgumentFactory<String> {
    NVarcharArgumentFactory() {
        super(Types.NVARCHAR);
    }

    @Override
    protected Argument build(String value, ConfigRegistry config) {
        return (pos, stmt, ctx) -> stmt.setNString(pos, value);
    }
}
