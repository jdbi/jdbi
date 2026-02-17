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
package org.jdbi.core.statement;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import org.jdbi.core.argument.Argument;
import org.jdbi.core.config.internal.ConfigCache;
import org.jdbi.core.config.internal.ConfigCaches;
import org.jdbi.core.internal.JdbiClassUtils;

class DescribedArgument implements Argument {
    private static final ConfigCache<Class<?>, Boolean> ARG_CLASS_HAS_TOSTRING =
            ConfigCaches.declare(type -> JdbiClassUtils.safeMethodLookup(type, "toString")
                    .map(Method::getDeclaringClass)
                    .map(c -> c != Object.class)
                    .orElse(false));
    private final Argument arg;
    private final String description;

    DescribedArgument(Argument arg, Object value) {
        this.arg = arg;
        this.description = Objects.toString(ArgumentBinder.unwrap(value));
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        arg.apply(position, statement, ctx);
    }

    @Override
    public String toString() {
        return description;
    }

    public static Argument wrap(StatementContext ctx, Argument arg, Object value) {
        if (Boolean.TRUE.equals(ARG_CLASS_HAS_TOSTRING.get(arg.getClass(), ctx))) {
            return arg;
        }
        return new DescribedArgument(arg, value);
    }
}
