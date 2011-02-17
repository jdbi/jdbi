/*
 * Copyright 2004 - 2011 Brian McCallister
 *
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

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.NamedArgumentFinder;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Binds all fields of a map as arguments.
 */
class MapArguments implements NamedArgumentFinder
{
    private final Map<String, ? extends Object> args;

    MapArguments(Map<String, ? extends Object> args)
    {
        this.args = args;
    }

    public Argument find(String name)
    {
        if (args.containsKey(name))
        {
            return new ObjectArgument(args.get(name));
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
