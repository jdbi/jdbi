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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

/**
 * Base {@link NamedArgumentFinder} implementation that can be used when binding properties of an object, with an
 * optional prefix.
 */
abstract class ObjectPropertyNamedArgumentFinder implements NamedArgumentFinder {
    final String prefix;
    final Object object;

    private final Map<String, Optional<NamedArgumentFinder>> childArgumentFinders = new ConcurrentHashMap<>();

    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param object the object bind on
     */
    public ObjectPropertyNamedArgumentFinder(String prefix, Object object) {
        this.prefix = (prefix == null || prefix.isEmpty()) ? "" : prefix + ".";
        this.object = object;
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
                    .computeIfAbsent(parentName, pn ->
                        getValue(pn, ctx).map(v -> getNestedArgumentFinder(v.value)))
                    .flatMap(arg -> arg.find(childName, ctx));
            }

            return getValue(actualName, ctx)
                .map(tv -> ctx.findArgumentFor(tv.type, tv.value)
                    .orElseThrow(() -> new UnableToCreateStatementException(
                        String.format("No argument factory registered for type [%s] for element [%s] on [%s]",
                            tv.type,
                            name,
                            object),
                        ctx)));
        }

        return Optional.empty();
    }

    abstract Optional<TypedValue> getValue(String name, StatementContext ctx);

    abstract NamedArgumentFinder getNestedArgumentFinder(Object obj);

    static class TypedValue {
        public final QualifiedType type;
        public final Object value;

        public TypedValue(Type type, Set<Annotation> qualifiers, Object value) {
            this.type = QualifiedType.of(type, qualifiers);
            this.value = value;
        }
    }
}
