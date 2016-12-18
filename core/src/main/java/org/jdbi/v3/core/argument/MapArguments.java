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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.statement.StatementContext;

/**
 * Binds all entries of a map as arguments.
 */
public class MapArguments implements NamedArgumentFinder
{
    private final Map<String, ?> args;
    private final StatementContext ctx;

    public MapArguments(Map<String, ?> args, StatementContext ctx)
    {
        this.ctx = ctx;
        this.args = args;
    }

    @Override
    public Optional<Argument> find(String name)
    {
        if (args.containsKey(name))
        {
            final Object argument = args.get(name);
            final Class<?> argumentClass =
                    argument == null ? Object.class : argument.getClass();
            return ctx.findArgumentFor(argumentClass, argument);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return new LinkedHashMap<>(args).toString();
    }
}
