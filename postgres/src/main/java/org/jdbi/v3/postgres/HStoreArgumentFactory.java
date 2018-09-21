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
import java.util.Map;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * An argument factory which binds Java's {@link Map} to Postgres' hstore type.
 */
// We must use a raw type to ensure we match all Map types rather than any particular generic type
@SuppressWarnings("rawtypes")
@HStore
public class HStoreArgumentFactory extends AbstractArgumentFactory<Map> {
    public HStoreArgumentFactory() {
        super(Types.OTHER);
    }

    @Override
    protected Argument build(Map value, ConfigRegistry config) {
        return (i, p, cx) -> p.setObject(i, value);
    }
}
