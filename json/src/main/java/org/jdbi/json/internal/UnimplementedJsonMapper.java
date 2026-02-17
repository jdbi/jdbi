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
package org.jdbi.json.internal;

import java.lang.reflect.Type;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.statement.UnableToCreateStatementException;
import org.jdbi.json.JsonConfig;
import org.jdbi.json.JsonMapper;

public class UnimplementedJsonMapper implements JsonMapper {
    private static final String NO_IMPL_INSTALLED = String.format(
        "you need to install (see %s) a %s impl, like jdbi3-jackson2 or jdbi3-gson2",
        JsonConfig.class.getSimpleName(),
        JsonMapper.class.getSimpleName()
    );

    @Override
    public TypedJsonMapper forType(Type type, ConfigRegistry config) {
        throw new UnableToCreateStatementException(NO_IMPL_INSTALLED);
    }
}
