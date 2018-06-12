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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.NamedArgumentFinder;

import static org.jdbi.v3.core.internal.JdbiStreams.toStream;

/**
 * Represents the arguments bound to a particular statement.
 */
public class Binding {
    private final Map<Integer, Argument> positionals = new HashMap<>();
    private final Map<String, Argument> named = new HashMap<>();
    private final List<NamedArgumentFinder> namedArgumentFinder = new ArrayList<>();

    /**
     * Bind a positional parameter at the given index (0-based)
     * @param position binding position
     * @param argument the argument to bind
     */
    public void addPositional(int position, Argument argument) {
        positionals.put(position, argument);
    }

    /**
     * Bind a named parameter for the given name
     * @param name bound argument name
     * @param argument the argument to bind
     */
    public void addNamed(String name, Argument argument) {
        this.named.put(name, argument);
    }

    /**
     * Bind a named argument finder
     * @param args the argument finder to bind
     */
    public void addNamedArgumentFinder(NamedArgumentFinder args) {
        namedArgumentFinder.add(args);
    }

    /**
     * Look up an argument by name
     *
     * @param name the key to lookup the value of
     * @param ctx the statement context
     *
     * @return the bound Argument
     */
    public Optional<Argument> findForName(String name, StatementContext ctx) {
        if (named.containsKey(name)) {
            return Optional.of(named.get(name));
        }

        return namedArgumentFinder.stream()
                .flatMap(arguments -> toStream(arguments.find(name, ctx)))
                .findFirst();
    }

    /**
     * Look up an argument by position
     *
     * @param position starts at 0, not 1
     *
     * @return argument bound to that position
     */
    public Optional<Argument> findForPosition(int position) {
        return Optional.ofNullable(positionals.get(position));
    }

    @Override
    @SuppressWarnings({"PMD.ConsecutiveAppendsShouldReuse", "PMD.ConsecutiveLiteralAppends"})
    public String toString() {
        boolean wrote = false;
        StringBuilder b = new StringBuilder();

        b.append("{ positional:{");
        for (Map.Entry<Integer, Argument> entry : positionals.entrySet()) {
            wrote = true;
            b.append(entry.getKey()).append(':').append(entry.getValue()).append(',');
        }
        if (wrote) {
            wrote = false;
            b.deleteCharAt(b.length() - 1);
        }
        b.append('}');

        b.append(", named:{");
        for (Map.Entry<String, Argument> entry : named.entrySet()) {
            wrote = true;
            b.append(entry.getKey()).append(':').append(entry.getValue()).append(',');
        }
        if (wrote) {
            wrote = false;
            b.deleteCharAt(b.length() - 1);
        }
        b.append('}');

        b.append(", finder:[");
        for (NamedArgumentFinder argument : namedArgumentFinder) {
            wrote = true;
            b.append(argument).append(',');
        }
        if (wrote) {
            b.deleteCharAt(b.length() - 1);
        }
        b.append(']');

        b.append('}');

        return b.toString();
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
     * @return true if there are no bindings yet
     */
    public boolean isEmpty() {
        return positionals.isEmpty() && named.isEmpty() && namedArgumentFinder.isEmpty();
    }
}
