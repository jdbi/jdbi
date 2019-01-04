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

import java.util.Optional;
import org.jdbi.v3.core.argument.internal.MethodReturnValueNamedArgumentFinder;
import org.jdbi.v3.core.argument.internal.TypedValue;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.mapper.reflect.internal.BeanPropertiesFactory;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;

/**
 * Inspect a {@link java.beans} style object and bind parameters
 * based on each of its discovered properties.
 *
 * @deprecated this should never have been public API
 */
@Deprecated
public class BeanPropertyArguments extends MethodReturnValueNamedArgumentFinder {
    private final PojoProperties<?> properties;

    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param bean the bean to inspect and bind
     */
    public BeanPropertyArguments(String prefix, Object bean) {
        super(prefix, bean);
        properties = BeanPropertiesFactory.propertiesFor(obj.getClass());
    }

    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param bean the bean to inspect and bind
     */
    protected BeanPropertyArguments(String prefix, Object bean, PojoProperties<?> properties) {
        super(prefix, bean);
        this.properties = properties;
    }

    @Override
    protected Optional<TypedValue> getValue(String name, StatementContext ctx) {
        @SuppressWarnings("unchecked")
        PojoProperty<Object> property = (PojoProperty<Object>) properties.getProperties().get(name);

        if (property == null) {
            return Optional.empty();
        }

        return Optional.of(new TypedValue(property.getQualifiedType(), property.get(obj)));
    }

    @Override
    protected NamedArgumentFinder getNestedArgumentFinder(Object o) {
        return new BeanPropertyArguments(null, o);
    }

    @Override
    public String toString() {
        return "{lazy bean property arguments \"" + obj + "\"}";
    }
}
