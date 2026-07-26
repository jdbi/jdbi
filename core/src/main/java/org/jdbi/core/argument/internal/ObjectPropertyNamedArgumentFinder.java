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
package org.jdbi.core.argument.internal;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.core.argument.Argument;
import org.jdbi.core.argument.Arguments;
import org.jdbi.core.argument.NamedArgumentFinder;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.core.statement.UnableToCreateStatementException;

/**
 * Base {@link NamedArgumentFinder} implementation that can be used when binding properties of an object, with an
 * optional prefix.
 */
public abstract class ObjectPropertyNamedArgumentFinder implements NamedArgumentFinder {
    protected final String prefix;
    protected final Object obj;

    private final Map<String, Optional<NamedArgumentFinder>> childArgumentFinders = new ConcurrentHashMap<>();

    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param obj the object bind on
     */
    protected ObjectPropertyNamedArgumentFinder(final String prefix, final Object obj) {
        this.prefix = prefix == null || prefix.isEmpty() ? "" : prefix + ".";
        this.obj = obj;
    }

    @Override
    public final Optional<Argument> find(final String name, final ConfigRegistry config) {
        if (name.startsWith(prefix)) {
            final String actualName = name.substring(prefix.length());

            final int separator = actualName.indexOf('.');

            if (separator != -1) {
                final String parentName = actualName.substring(0, separator);
                final String childName = actualName.substring(separator + 1);

                return childArgumentFinders
                    .computeIfAbsent(parentName.endsWith("?") ? parentName.substring(0, parentName.length() - 1) : parentName, pn ->
                        getValue(pn, config).map(typedValue -> getValueNested(typedValue, parentName, childName)))
                    .flatMap(arg -> arg.find(childName, config));
            }

            return getValue(actualName, config)
                .map(tv -> config.findArgumentFor(tv.getType(), tv.getValue())
                    .orElseThrow(() -> new UnableToCreateStatementException(
                        String.format("No argument factory registered for type [%s] for element [%s] on [%s]",
                            tv.getType(),
                            name,
                            obj))));
        }

        return Optional.empty();
    }

    private NamedArgumentFinder getValueNested(final TypedValue typedValue, final String parentName, final String childName) {
        if (Objects.nonNull(typedValue.getValue())) {
            return getNestedArgumentFinder(typedValue);
        }
        if (parentName.endsWith("?")) {
            return (n, c) -> Optional.of(c.get(Arguments.class).getUntypedNullArgument());
        }
        throw new IllegalArgumentException(
            String.format("Trying to bind nested argument [%s], but found null value at [%s], may mark it as an optional with [%s]",
                childName,
                parentName,
                parentName + '?'));
    }

    protected abstract Optional<TypedValue> getValue(String name, ConfigRegistry ctx);
    protected abstract NamedArgumentFinder getNestedArgumentFinder(TypedValue obj);
}
