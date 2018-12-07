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

import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties.PojoProperty;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

/**
 * Inspect a object and bind parameters via {@link BeanMapper}'s properties.
 */
public class PojoPropertyArguments extends MethodReturnValueNamedArgumentFinder {
    private final Map<String, ? extends PojoProperty<?>> properties;
    private final StatementContext ctx;

    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param bean the bean to inspect and bind
     * @param ctx the statement context
     */
    public PojoPropertyArguments(String prefix, Object bean, StatementContext ctx) {
        super(prefix, bean);
        this.ctx = ctx;
        final RowMapper<? extends Object> mapper = ctx.findRowMapperFor(bean.getClass())
                .orElseThrow(() -> new UnableToCreateStatementException("Couldn't find registered property mapper for " + bean.getClass()));
        if (!(mapper instanceof BeanMapper<?>)) {
            throw new UnableToCreateStatementException("Registered mapper for " + bean.getClass() + " is not a property based mapper");
        }
        properties = ((BeanMapper<?>) mapper).getBeanInfo().getProperties();
    }

    @Override
    protected Optional<TypedValue> getValue(String name, StatementContext ctx2) {
        @SuppressWarnings("unchecked")
        PojoProperty<Object> property = (PojoProperty<Object>) properties.get(name);
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
