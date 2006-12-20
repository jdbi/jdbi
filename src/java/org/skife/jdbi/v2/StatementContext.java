/*
 * Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2;

import java.util.Map;
import java.util.HashMap;

/**
 * The statement context provides a means for passing client specific information through the
 * evaluation of a statement. The context is not used by jDBI internally, but will be passed
 * to all statement customizers. This makes it possible to parameterize the processing of
 * the tweakable parts of the statement processing cycle.
 */
public class StatementContext
{
    private final Map<String, Object> attributes = new HashMap<String, Object>();

    StatementContext(Map<String, Object> globalAttributes) {
        attributes.putAll(globalAttributes);
    }

    /**
     * Specify an attribute on the statement context
     *
     * @param key   name of the attribute
     * @param value value for the attribute
     *
     * @return previous value of this attribute
     */
    public Object setAttribute(String key, Object value)
    {
        return attributes.put(key, value);
    }

    /**
     * Obtain the value of an attribute
     *
     * @param key The name of the attribute
     * @return the value of the attribute
     */
    public Object getAttribute(String key)
    {
        return this.attributes.get(key);
    }

    /**
     * Obtain all the attributes associated with this context as a map. Changes to the map
     * or to the attributes on the context will be reflected across both
     * @return a map f attributes
     */
    public Map<String, Object> getAttributes()
    {
        return attributes;
    }
}
