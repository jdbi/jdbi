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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jdbi.v3.tweak.Argument;
import org.jdbi.v3.tweak.ArgumentFactory;

class Foreman
{

    private final List<ArgumentFactory<?>> factories = new CopyOnWriteArrayList<>();

    Foreman()
    {
        factories.add(BuiltInArgumentFactory.INSTANCE);
    }

    Foreman(List<ArgumentFactory<?>> factories)
    {
        this.factories.addAll(factories);
    }

    Argument waffle(Class<?> expectedType, Object it, StatementContext ctx)
    {
        ArgumentFactory<Object> candidate = null;

        for (int i = factories.size() - 1; i >= 0; i--) {
            @SuppressWarnings("unchecked")
            ArgumentFactory<Object> factory = (ArgumentFactory<Object>) factories.get(i);

            if (factory.accepts(expectedType, it, ctx)) {
                return factory.build(expectedType, it, ctx);
            }
            // Fall back to any factory accepting Object if necessary but
            // prefer any more specific factory first.
            if (candidate == null && factory.accepts(Object.class, it, ctx)) {
                candidate = factory;
            }
        }
        if (candidate != null) {
            return candidate.build(Object.class, it, ctx);
        }

        throw new IllegalStateException("Unbindable argument passed: " + String.valueOf(it));
    }

    void register(ArgumentFactory<?> argumentFactory)
    {
        factories.add(argumentFactory);
    }

    Foreman createChild()
    {
        return new Foreman(factories);
    }
}
