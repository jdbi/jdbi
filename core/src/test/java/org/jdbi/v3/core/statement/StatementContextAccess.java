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
package org.jdbi.v3.core.statement;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.UtilityClassException;

public final class StatementContextAccess {

    private StatementContextAccess() {
        throw new UtilityClassException();
    }

    public static StatementContext createContext() {
        return StatementContext.create(new ConfigRegistry(), null, StatementContextAccess.class);
    }

    public static StatementContext createContext(final ConfigRegistry config) {
        return StatementContext.create(config, null, StatementContextAccess.class);
    }

    /**
     * Create a simple statement context that shares configuration
     * with the given handle.
     */
    public static StatementContext createContext(final Handle handle) {
        return StatementContext.create(handle.getConfig(), null, StatementContextAccess.class);
    }
}
