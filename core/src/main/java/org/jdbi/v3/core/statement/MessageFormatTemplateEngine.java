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
 * Uses {@link MessageFormat#format} as a template engine.
 *
 * You should use "0", "1", "2", etc as keys. You can {@link org.jdbi.v3.core.config.Configurable#define} values in any order.
 *
 * Start at 0, increment by 1, do not repeat any keys, and do not exceed the maximum array size for your system. Leading zeroes are ignored. Invalid keys will trigger an {@link IllegalArgumentException} when {@link #render} is called.
 *
 * Note: MessageFormat does not throw exceptions when your input's placeholders don't match the values, and neither does this class. This class only validates your keys, not the input string or values.
 *
 * Example usage:
 * <pre>{@code
 *     // select bar from foo
 *     jdbi.useHandle(handle -> handle.createCall("select {1} from {0}")
 *         .setTemplateEngine(MessageFormatTemplateEngine.INSTANCE)
 *         .define("0", "foo")
 *         .define("1", "bar")
 *         .invoke());
 * }</pre>
 */
public enum MessageFormatTemplateEngine implements TemplateEngine {
    INSTANCE;

    public static void main(String[] args) {
        MessageFormat.format("{1} {0}", new Object[0]);
    }

    @Override
    public String render(String template, StatementContext ctx) {
        validateKeys(ctx.getAttributes().keySet());

        Object[] args = ctx.getAttributes()
            .entrySet()
            .stream()
            .map(x -> new AbstractMap.SimpleImmutableEntry<>(Integer.valueOf(x.getKey()), x.getValue()))
            .sorted(Comparator.comparingInt(AbstractMap.SimpleImmutableEntry::getKey))
            .map(AbstractMap.SimpleImmutableEntry::getValue)
            .toArray(Object[]::new);

        return MessageFormat.format(template, args);
    }

    private static void validateKeys(Set<String> keySet) {
        if (keySet.size() == 0) {
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

        int last = 0;
        for (int i = 1; i < keys.length; i++) {
            final int key = keys[i];

            if (key == last) {
                throw new IllegalArgumentException("key " + key + " was given more than once");
            }

            if (key > last + 1) {
                throw new IllegalArgumentException("keys skip from " + last + " to " + key);
            }

            last = key;
        }
    }
}
