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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ArgumentFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Foreman will select best ArgumentFactory to use from all the registered
 * ArgumentFactory instances and return an Argument created from that
 * ArgumentFactory.
 */
public class Foreman
{
    private final List<ArgumentFactory> factories = new CopyOnWriteArrayList<ArgumentFactory>();

    Foreman()
    {
        factories.add(BUILT_INS);
    }

    Foreman(List<ArgumentFactory> factories)
    {
        this.factories.addAll(factories);
    }

    /**
     *
     * @param expectedType The type to use for matching against bound ArgumentFactory instances
     * @param boundValue The value to be bound by the created Argument
     * @param ctx the relevant StatementContext
     * @return
     */
    public Argument createArgument(Class<?> expectedType, Object boundValue, StatementContext ctx)
    {
        ArgumentFactory candidate = null;

        for (int i = factories.size() - 1; i >= 0; i--) {
            ArgumentFactory factory = factories.get(i);
            if (factory.accepts(expectedType, boundValue, ctx)) {
                return factory.build(expectedType, boundValue, ctx);
            }
            // Fall back to any factory accepting Object if necessary but
            // prefer any more specific factory first.
            if (candidate == null && factory.accepts(Object.class, boundValue, ctx)) {
                candidate = factory;
            }
        }
        if (candidate != null) {
            return candidate.build(Object.class, boundValue, ctx);
        }

        throw new IllegalStateException("Unbindable argument passed: " + String.valueOf(boundValue));
    }

    private static final ArgumentFactory BUILT_INS = new BuiltInArgumentFactory();

    void register(ArgumentFactory<?> argumentFactory)
    {
        factories.add(argumentFactory);
    }

    Foreman createChild()
    {
        return new Foreman(factories);
    }


}
