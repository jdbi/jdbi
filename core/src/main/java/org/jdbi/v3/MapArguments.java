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
package org.jdbi.v3;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.NamedArgumentFinder;

/**
 * Binds all fields of a map as arguments.
 */
class MapArguments implements NamedArgumentFinder
{
    private final Foreman foreman;
    private final StatementContext ctx;
    private final Map<String, ?> args;

    MapArguments(Foreman foreman, StatementContext ctx, Map<String, ?> args)
    {
        this.foreman = foreman;
        this.ctx = ctx;
        this.args = args;
    }

    @Override
    public Argument find(String name)
    {
        if (args.containsKey(name))
        {
            final Object argument = args.get(name);
            final Class<?> argumentClass =
                    argument == null ? Object.class : argument.getClass();
            return foreman.waffle(argumentClass, argument, ctx);
        }
        else
        {
            return null;
        }
    }

    @Override
    public String toString() {
        return new LinkedHashMap<String, Object>(args).toString();
    }
}
