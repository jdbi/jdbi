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
package org.jdbi.v3.core.argument.internal;

import java.util.Optional;

import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;
import org.jdbi.v3.core.mapper.reflect.internal.PojoPropertiesFactories;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * This class hosts the logic from BeanPropertyArguments.
 * When we can remove that class from public API, this class will easily replace it.
 */
public class PojoPropertyArguments extends MethodReturnValueNamedArgumentFinder {
    private final PojoProperties<?> properties;
    private final StatementContext ctx;

    public PojoPropertyArguments(String prefix, Object obj, StatementContext ctx) {
        this(prefix, obj, ctx.getConfig(PojoPropertiesFactories.class).propertiesOf(obj.getClass()), ctx);
    }

    protected PojoPropertyArguments(String prefix, Object obj, PojoProperties<?> properties, StatementContext ctx) {
        super(prefix, obj);
        this.properties = properties;
        this.ctx = ctx;
    }

    @Override
    protected Optional<TypedValue> getValue(String name, StatementContext ctx2) {
        @SuppressWarnings("unchecked")
        PojoProperty<Object> property = (PojoProperty<Object>) properties.getProperties().get(name);

        if (property == null) {
            return Optional.empty();
        }

        return Optional.of(new TypedValue(property.getQualifiedType(), property.get(obj)));
    }

    @Override
    protected NamedArgumentFinder getNestedArgumentFinder(Object o) {
        return new PojoPropertyArguments(null, o, ctx);
    }

    @Override
    public String toString() {
        return "{lazy bean property arguments \"" + obj + "\"}";
    }
}
