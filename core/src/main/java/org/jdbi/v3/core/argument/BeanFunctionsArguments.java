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

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Binds public functions with no parameters on a specified object.
 */
public class BeanFunctionsArguments extends FunctionReturnValueNamedArgumentFinder
{
    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param object the object to bind functions on
     */
    public BeanFunctionsArguments(String prefix, Object object)
    {
        super(prefix, object);
    }

    @Override
    public Optional<Argument> find0(String name, StatementContext ctx)
    {
        if (name.startsWith(prefix))
        {
            String propertyName = name.substring(prefix.length());

            for (Method method : object.getClass().getMethods())
            {
                if (method.getParameterCount() == 0 && method.getName().equals(propertyName))
                {
                    return getArgumentForMethod(method, ctx);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "{lazy object functions arguments \"" + object + "\"";
    }
}
