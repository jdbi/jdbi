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

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
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

    Argument waffle(Type expectedType, Object it, StatementContext ctx)
    {
        return waffle(new TypeResolver().resolve(expectedType), it, ctx);
    }

    Argument waffle(ResolvedType expectedType, Object it, StatementContext ctx)
    {
        ResolvedType objectType = new TypeResolver().resolve(Object.class);

        ArgumentFactory<Object> candidate = null;

        for (int i = factories.size() - 1; i >= 0; i--) {
            @SuppressWarnings("unchecked")
            ArgumentFactory<Object> factory = (ArgumentFactory<Object>) factories.get(i);

            if (factory.accepts(expectedType, it, ctx)) {
                return factory.build(expectedType, it, ctx);
            }
            // Fall back to any factory accepting Object if necessary but
            // prefer any more specific factory first.
            if (candidate == null && factory.accepts(objectType, it, ctx)) {
                candidate = factory;
            }
        }
        if (candidate != null) {
            return candidate.build(objectType, it, ctx);
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
