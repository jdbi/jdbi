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

import org.jdbi.v3.core.statement.StatementContext;

import java.util.Optional;

/**
 * Base {@link NamedArgumentFinder} implementation that can be used when binding properties of an object, with an
 * optional prefix.
 */
abstract class ObjectPropertyNamedArgumentFinder implements NamedArgumentFinder
{
    protected final String prefix;
    protected final Object object;

    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param object the object bind on
     */
    public ObjectPropertyNamedArgumentFinder(String prefix, Object object)
    {
        this.prefix = (prefix == null || prefix.isEmpty()) ? "" : prefix + ".";
        this.object = object;
    }

    @Override
    public final Optional<Argument> find(String name, StatementContext ctx)
    {
        if (name.startsWith(prefix))
        {
            final String actualName = name.substring(prefix.length());

            return find0(actualName, ctx);
        }

        return Optional.empty();
    }

    /**
     * @see #find(String, StatementContext)
     * @param name name of the property to bind (this does *not* include the prefix)
     * @param ctx {@link StatementContext} to bind on
     */
    abstract Optional<Argument> find0(String name, StatementContext ctx);
}
