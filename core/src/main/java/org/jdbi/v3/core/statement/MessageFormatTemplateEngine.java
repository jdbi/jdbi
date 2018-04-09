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

/**
 * Uses {@link MessageFormat#format} as a template engine.
 *
 * You need to use "0", "1", "2", etc as keys. You can {@link org.jdbi.v3.core.config.Configurable#define} values in any order.
 *
 * Start at 0, increment by 1, do not repeat any keys, and do not exceed the maximum array size for your system. Leading zeroes are ignored.
 *
 * Keys are NOT checked for semantic or technical correctness — bad keys lead to undefined behavior that may include {@link RuntimeException}s, being ignored, overwriting other values, and wrong ordering,
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

    @Override
    public String render(String template, StatementContext ctx) {
        Object[] args = ctx.getAttributes()
            .entrySet()
            .stream()
            .map(x -> new AbstractMap.SimpleImmutableEntry<>(Integer.valueOf(x.getKey()), x.getValue()))
            .sorted(Comparator.comparingInt(AbstractMap.SimpleImmutableEntry::getKey))
            .map(AbstractMap.SimpleImmutableEntry::getValue)
            .toArray(Object[]::new);

        return MessageFormat.format(template, args);
    }
}
