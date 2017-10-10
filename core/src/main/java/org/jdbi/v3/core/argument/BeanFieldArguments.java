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

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

/**
 * Inspect an object and binds parameters based on each of its public fields.
 */
public class BeanFieldArguments extends ObjectPropertyNamedArgumentFinder
{
    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param bean the bean to inspect and bind
     */
    public BeanFieldArguments(String prefix, Object bean)
    {
        super(prefix, bean);
    }

    @Override
    public Optional<Argument> find0(String name, StatementContext ctx)
    {
        try
        {
            for (Field field : object.getClass().getFields())
            {
                if (field.getName().equals(name))
                {
                    Object fieldValue = field.get(object);
                    Type fieldType = field.getGenericType();
                    Optional<Argument> argument = ctx.findArgumentFor(fieldType, fieldValue);

                    if (!argument.isPresent())
                    {
                        throw new UnableToCreateStatementException(
                                String.format("No argument factory registered for type [%s] for field [%s] on [%s]",
                                        fieldType,
                                        name,
                                        object), ctx);
                    }

                    return argument;
                }
            }
        }
        catch (IllegalAccessException e)
        {
            throw new UnableToCreateStatementException(String.format("Access exception getting field for " +
                            "bean property [%s] on [%s]",
                    name, object), e, ctx);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "{lazy bean field arguments \"" + object + "\"";
    }
}

