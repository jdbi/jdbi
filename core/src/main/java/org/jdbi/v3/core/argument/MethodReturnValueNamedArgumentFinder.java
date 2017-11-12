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
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Base {@link NamedArgumentFinder} implementation that can be used for bindings that use the return value
 * of an object's method as an argument.
 */
abstract class MethodReturnValueNamedArgumentFinder extends ObjectPropertyNamedArgumentFinder
{
    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param object the object to bind methods on
     */
    MethodReturnValueNamedArgumentFinder(String prefix, Object object)
    {
        super(prefix, object);
    }

    Object invokeMethod(Method method, StatementContext ctx) {
        try
        {
            return method.invoke(object);
        }
        catch (IllegalAccessException e)
        {
            throw new UnableToCreateStatementException(String.format("Access exception invoking " +
                            "method [%s] on [%s]",
                    method.getName(), object), e, ctx);
        }
        catch (InvocationTargetException e)
        {
            throw new UnableToCreateStatementException(String.format("Invocation target exception invoking " +
                            "method [%s] on [%s]",
                    method.getName(), object), e, ctx);
        }
    }
}
