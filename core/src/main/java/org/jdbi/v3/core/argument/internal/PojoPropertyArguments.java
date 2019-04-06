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

import java.util.Collection;
import java.util.Optional;

import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;
import org.jdbi.v3.core.mapper.reflect.internal.PojoTypes;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

/**
 * This class hosts the logic from BeanPropertyArguments.
 * When we can remove that class from public API, this class will easily replace it.
 */
public class PojoPropertyArguments extends MethodReturnValueNamedArgumentFinder {
    private final PojoProperties<?> properties;
    private final ConfigRegistry config;

    public PojoPropertyArguments(String prefix, Object obj, ConfigRegistry config) {
        this(prefix,
                obj,
                config.get(PojoTypes.class).findFor(obj.getClass())
                    .orElseThrow(() -> new UnableToCreateStatementException("Couldn't find pojo type of " + obj.getClass())),
                config);
    }

    protected PojoPropertyArguments(String prefix, Object obj, PojoProperties<?> properties, ConfigRegistry config) {
        super(prefix, obj);
        this.properties = properties;
        this.config = config;
    }

    @Override
    protected Optional<TypedValue> getValue(String name, StatementContext ctx2) {
        @SuppressWarnings("unchecked")
        PojoProperty<Object> property = (PojoProperty<Object>) properties.getProperties().get(name);
        return Optional.ofNullable(property)
                .map(p -> new TypedValue(p.getQualifiedType(), p.get(obj)));
    }

    @Override
    public Collection<String> getNames() {
        return properties.getProperties().keySet();
    }

    @Override
    protected NamedArgumentFinder getNestedArgumentFinder(Object o) {
        return new PojoPropertyArguments(null, o, config);
    }

    @Override
    public String toString() {
        return "{lazy bean property arguments \"" + obj + "\"}";
    }
}
