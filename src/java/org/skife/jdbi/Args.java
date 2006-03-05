/* Copyright 2004-2005 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi;

import java.util.HashMap;

/**
 * Convenience class for building named argument maps
 */
public class Args extends HashMap
{
    /**
     * Create a new Args instance, typically to be used for named parameters
     *
     * @param name named parameter in statement
     * @param arg value to bindBinaryStream
     * @return an Args instance, which happens to be a Map
     */
    public static Args with(final String name, final Object arg)
    {
        final Args args = new Args();
        args.put(name, arg);
        return args;
    }

    /**
     * Add another named argument to the Args
     *
     * @param name named parameter in statement
     * @param arg value to bindBinaryStream
     * @return self
     */
    public Args and(final String name, final Object arg)
    {
        this.put(name, arg);
        return this;
    }
}
