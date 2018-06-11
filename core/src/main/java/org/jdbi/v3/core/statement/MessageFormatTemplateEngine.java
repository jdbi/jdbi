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

import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Set;

/**
 * Uses the equivalent of {@link MessageFormat#format(String, Object...)} as a template engine.
 *
 * You must use "0", "1", "2", etc as keys: start at 0, increment by 1. Keys must be numerically unique. You must {@link org.jdbi.v3.core.config.Configurable#define(String, Object)} as many key/value pairs as there are placeholders in the pattern string.
 *
 * You may {@code define} key/value pairs in any order. Keys may contain leading {@code '0'}s.
 *
 * Any invalid use will trigger an {@link IllegalArgumentException} (or subclasses such as {@link NumberFormatException}) when {@link #render(String, StatementContext)} is called – typically when the statement is about to be executed.
 *
 * Example usage:
 * <pre>{@code
 *     // select bar from foo where col = 'abc'
 *     jdbi.useHandle(handle -> handle.createCall("select {1} from {0} where col = ''{2}''")
 *         .setTemplateEngine(MessageFormatTemplateEngine.INSTANCE)
 *         .define("0", "foo")
 *         .define("1", "bar")
 *         .define("2", "abc")
 *         .invoke());
 * }</pre>
 */
public class MessageFormatTemplateEngine implements TemplateEngine {
    /**
     * @deprecated use the default constructor instead
     */
    @Deprecated
    public static final TemplateEngine INSTANCE = new MessageFormatTemplateEngine();

    public MessageFormatTemplateEngine() {}

    @Override
    public String render(String template, StatementContext ctx) {
        MessageFormat msgFormat = new MessageFormat(template);

        validateKeys(ctx.getAttributes().keySet(), msgFormat.getFormats().length);

        Object[] args = ctx.getAttributes()
            .entrySet()
            .stream()
            .map(x -> new AbstractMap.SimpleImmutableEntry<>(Integer.valueOf(x.getKey()), x.getValue()))
            .sorted(Comparator.comparingInt(AbstractMap.SimpleImmutableEntry::getKey))
            .map(AbstractMap.SimpleImmutableEntry::getValue)
            .toArray(Object[]::new);

        return msgFormat.format(args);
    }

    private static void validateKeys(Set<String> keySet, int expectedCount) {
        if (keySet.size() != expectedCount) {
            throw new IllegalArgumentException("expected " + expectedCount + " keys but got " + keySet.size());
        }

        if (keySet.isEmpty()) {
            return;
        }

        // keys inherently cannot be null, so we only need to check the content
        final int[] keys = keySet.stream()
            // throws IllegalArgumentException for us
            .mapToInt(Integer::parseInt)
            .sorted()
            .toArray();

        if (keys[0] != 0) {
            throw new IllegalArgumentException("lowest key must be 0");
        }

        for (int i = 1; i < keys.length; i++) {
            final int key = keys[i];

            if (key < i) {
                throw new IllegalArgumentException("key " + key + " was given more than once");
            }

            if (key > i) {
                throw new IllegalArgumentException("keys skip from " + (i - 1) + " to " + key);
            }
        }
    }
}
