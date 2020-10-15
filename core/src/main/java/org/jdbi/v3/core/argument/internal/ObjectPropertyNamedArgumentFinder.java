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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

/**
 * Base {@link NamedArgumentFinder} implementation that can be used when binding properties of an object, with an
 * optional prefix.
 */
public abstract class ObjectPropertyNamedArgumentFinder implements NamedArgumentFinder {
    private final String prefix;
    protected final Object obj;

    private final Map<String, Optional<NamedArgumentFinder>> childArgumentFinders = new ConcurrentHashMap<>();

    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param obj the object bind on
     */
    protected ObjectPropertyNamedArgumentFinder(String prefix, Object obj) {
        this.prefix = prefix == null || prefix.isEmpty() ? "" : prefix + ".";
        this.obj = obj;
    }

    @Override
    public final Optional<Argument> find(String name, StatementContext ctx) {
        if (name.startsWith(prefix)) {
            final String actualName = name.substring(prefix.length());

            int separator = actualName.indexOf('.');

            if (separator != -1) {
                String parentName = actualName.substring(0, separator);
                String childName = actualName.substring(separator + 1);

                return childArgumentFinders
                    .computeIfAbsent(parentName.endsWith("?") ? parentName.substring(0, parentName.length() - 1) : parentName, pn ->
                        getValue(pn, ctx).map(typedValue -> getValueNested(typedValue, parentName, childName)))
                    .flatMap(arg -> arg.find(childName, ctx));
            }

            return getValue(actualName, ctx)
                .map(tv -> ctx.findArgumentFor(tv.type, tv.value)
                    .orElseThrow(() -> new UnableToCreateStatementException(
                        String.format("No argument factory registered for type [%s] for element [%s] on [%s]",
                            tv.type,
                            name,
                            obj),
                        ctx)));
        }

        return Optional.empty();
    }

    private NamedArgumentFinder getValueNested(TypedValue typedValue, String parentName, String childName) {
        if (Objects.nonNull(typedValue.value)) {
            return getNestedArgumentFinder(typedValue);
        }
        if (parentName.endsWith("?")) {
            return (n, c) -> Optional.of(c.getConfig(Arguments.class).getUntypedNullArgument());
        }
        throw new IllegalArgumentException(
            String.format("Trying to bind nested argument [%s], but found nullpointer at [%s], may mark it as an optional with [%s]",
                childName,
                parentName,
                parentName + '?'));
    }

    protected abstract Optional<TypedValue> getValue(String name, StatementContext ctx);
    protected abstract NamedArgumentFinder getNestedArgumentFinder(TypedValue obj);
}
