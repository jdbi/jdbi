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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdbi.v3.core.annotation.internal.JdbiAnnotations;
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
public class PojoPropertyArguments extends ObjectPropertyNamedArgumentFinder {
    protected final PojoProperties<?> properties;
    protected final ConfigRegistry config;
    private final Set<String> names;

    public PojoPropertyArguments(String prefix, Object obj, Type type, ConfigRegistry config) {
        this(prefix,
                obj,
                config.get(PojoTypes.class).findFor(type)
                    .orElseThrow(() -> new UnableToCreateStatementException("Couldn't find pojo type of " + obj.getClass())),
                config);
    }

    protected PojoPropertyArguments(String prefix, Object obj, PojoProperties<?> properties, ConfigRegistry config) {
        super(prefix, obj);
        this.properties = properties;
        this.config = config;
        names = properties.getProperties()
                .entrySet()
                .stream()
                .filter(e -> JdbiAnnotations.isBound(e.getValue()))
                .map(Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Optional<TypedValue> getValue(String name, StatementContext ctx) {
        return Optional.ofNullable(properties.getProperties().get(name))
                .map(PojoProperty.class::cast)
                .filter(JdbiAnnotations::isBound)
                .map(p -> new TypedValue(p.getQualifiedType(), p.get(obj)));
    }

    @Override
    public Collection<String> getNames() {
        return names;
    }

    @Override
    protected NamedArgumentFinder getNestedArgumentFinder(TypedValue value) {
        return new PojoPropertyArguments(null, value.getValue(), value.getType().getType(), config);
    }

    @Override
    public String toString() {
        return "{lazy bean property arguments \"" + obj + "\"}";
    }
}
