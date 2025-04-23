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
package org.jdbi.v3.core.statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.argument.internal.TypedValue;
import org.jdbi.v3.core.qualifier.QualifiedType;

import static org.jdbi.v3.core.statement.ArgumentBinder.unwrap;
/**
 * Represents the arguments bound to a particular statement.
 */
public class Binding {
    protected final Map<Integer, Object> positionals = new TreeMap<>();
    protected final Map<String, Object> named = new LinkedHashMap<>();
    protected final List<NamedArgumentFinder> namedArgumentFinder = new ArrayList<>();
    private final StatementContext ctx;

    protected Binding(StatementContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Bind a positional parameter at the given index (0-based).
     * @param position binding position
     * @param argument the argument to bind
     */
    public void addPositional(int position, Argument argument) {
        addPositional(position, (Object) argument);
    }

    /**
     * Bind a named parameter for the given name.
     * @param name bound argument name
     * @param argument the argument to bind
     */
    public void addNamed(String name, Argument argument) {
        addNamed(name, (Object) argument);
    }

    /**
     * Bind a positional parameter at the given index (0-based).
     * @param position binding position
     * @param argument the argument to bind
     */
    public void addPositional(int position, Object argument) {
        positionals.put(position, argument);
    }

    /**
     * Bind a named parameter for the given name.
     * @param name bound argument name
     * @param argument the argument to bind
     */
    public void addNamed(String name, Object argument) {
        named.put(name, argument);
    }

    /**
     * Bind a positional parameter at the given index (0-based).
     * @param position binding position
     * @param argument the argument to bind
     */
    public void addPositional(int position, Object argument, QualifiedType<?> type) {
        positionals.put(position, new TypedValue(type, argument));
    }

    /**
     * Bind a named parameter for the given name.
     * @param name bound argument name
     * @param argument the argument to bind
     */
    public void addNamed(String name, Object argument, QualifiedType<?> type) {
        named.put(name, new TypedValue(type, argument));
    }

    /**
     * Bind a named argument finder.
     * @param args the argument finder to bind
     */
    public void addNamedArgumentFinder(NamedArgumentFinder args) {
        namedArgumentFinder.add(args);
    }

    /**
     * Look up an argument by name.
     *
     * @param name the key to lookup the value of
     * @param ctx2 the statement context
     *
     * @deprecated don't inspect a Binding: keep your own state!
     * @return the bound Argument
     */
    @Deprecated(since = "3.11.0", forRemoval = true)
    public Optional<Argument> findForName(String name, StatementContext ctx2) {
        final Object found = named.get(name);
        if (found != null || named.containsKey(name)) {
            return Optional.of(new ArgumentBinder(null, ctx2, ParsedParameters.NONE).toArgument(found));
        }

        return namedArgumentFinder.stream()
                .flatMap(arguments -> arguments.find(name, ctx2).stream())
                .findFirst();
    }

    /**
     * @return the set of known binding names
     * @deprecated this is expensive to compute, and it's bad practice to inspect a Binding: keep track of your own state!
     */
    @Deprecated(since = "3.11.0", forRemoval = true)
    public Collection<String> getNames() {
        final Set<String> names = new HashSet<>(named.keySet());
        namedArgumentFinder.forEach(args -> names.addAll(args.getNames()));
        return Collections.unmodifiableSet(names);
    }

    /**
     * Look up an argument by position.
     *
     * @param position starts at 0, not 1
     * @deprecated don't inspect a Binding: keep your own state!
     * @return argument bound to that position
     */
    @Deprecated(since = "3.11.0", forRemoval = true)
    public Optional<Argument> findForPosition(int position) {
        return Optional.ofNullable(new ArgumentBinder(null, ctx, ParsedParameters.NONE).toArgument(positionals.get(position)));
    }

    @Override
    public String toString() {
        return describe(Integer.MAX_VALUE);
    }

    /**
     * Generate toString but with a maximum length.
     * This is used to generate a preview even if the arguments may be very long.
     * @param maxLength The maximum length of string to return
     * @return toString truncated to maxLength
     */
    public String describe(final int maxLength) {
        boolean firstSection = true;
        boolean firstElem = true;
        final StringBuilder buf = new StringBuilder();
        buf.append('{');
        if (!positionals.isEmpty()) {
            firstSection = false;
            buf.append("pos:{");
            for (final var e : positionals.entrySet()) {
                if (!firstElem) {
                    buf.append(',');
                }
                firstElem = false;
                buf.append(e.getKey())
                    .append(':')
                    .append(unwrap(e.getValue()));
                if (buf.length() > maxLength) {
                    break;
                }
            }
            buf.append('}');
        }
        if (!named.isEmpty() && buf.length() < maxLength) {
            if (!firstSection) {
                buf.append(", ");
            }
            firstSection = false;
            firstElem = true;
            buf.append("named:{");
            for (final var e : named.entrySet()) {
                if (!firstElem) {
                    buf.append(',');
                }
                firstElem = false;
                buf.append(e.getKey())
                    .append(':')
                    .append(unwrap(e.getValue()));
                if (buf.length() > maxLength) {
                    break;
                }
            }
            buf.append('}');
        }
        if (!namedArgumentFinder.isEmpty() && buf.length() < maxLength) {
            if (!firstSection) {
                buf.append(", ");
            }
            firstElem = true;
            buf.append("finder:[");
            for (final var naf : namedArgumentFinder) {
                if (!firstElem) {
                    buf.append(',');
                }
                firstElem = false;
                buf.append(naf);
                if (buf.length() > maxLength) {
                    break;
                }
            }
            buf.append('}');
        }
        if (buf.length() > maxLength - 2) {
            buf.setLength(maxLength - 2);
            buf.append('…');
        }
        return buf + "}";
    }

    /**
     * Remove all bindings from this Binding.
     */
    public void clear() {
        positionals.clear();
        named.clear();
        namedArgumentFinder.clear();
    }

    /**
     * Returns whether any bindings exist.
     *
     * @return True if there are no bindings.
     */
    public boolean isEmpty() {
        return positionals.isEmpty() && named.isEmpty() && namedArgumentFinder.isEmpty();
    }
}
