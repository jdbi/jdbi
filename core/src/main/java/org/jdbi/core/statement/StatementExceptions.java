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
package org.jdbi.core.statement;

import java.util.function.Function;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.meta.Beta;

/**
 * Configuration for {@link StatementException} and subclasses behavior.
 */
@Beta
public class StatementExceptions implements JdbiConfig<StatementExceptions> {

    private final Function<StatementException, String> messageRendering;
    private final int lengthLimit;

    public StatementExceptions() {
        this(MessageRendering.SHORT_STATEMENT, 1024);
    }

    private StatementExceptions(Function<StatementException, String> messageRendering, int lengthLimit) {
        this.messageRendering = messageRendering;
        this.lengthLimit = lengthLimit;
    }

    /**
     * Returns the limit hint to use to shorten strings.
     *
     * @return the limit hint to use to shorten strings.
     */
    public int getLengthLimit() {
        return lengthLimit;
    }

    /**
     * Returns a copy of this configuration with the given hint on how long to shorten variable-length strings to.
     *
     * @param lengthLimit the limit hint.
     * @return the derived configuration
     */
    @CheckReturnValue
    public StatementExceptions lengthLimit(int lengthLimit) {
        return new StatementExceptions(messageRendering, lengthLimit);
    }

    /**
     * Returns the statement exception message rendering strategy.
     *
     * @return the statement exception message rendering strategy.
     * @see MessageRendering
     */
    public Function<StatementException, String> getMessageRendering() {
        return messageRendering;
    }

    /**
     * Returns a copy of this configuration with the given exception statement message generation strategy.
     * @param messageRendering the message rendering strategy to use
     * @return the derived configuration
     * @see MessageRendering
     */
    @CheckReturnValue
    public StatementExceptions messageRendering(Function<StatementException, String> messageRendering) {
        return new StatementExceptions(messageRendering, lengthLimit);
    }

    @Override
    public StatementExceptions createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }

    /**
     * Control exception message generation.
     */
    public enum MessageRendering implements Function<StatementException, String> {
        /**
         * Do not include SQL or parameter information.
         */
        NONE {
            @Override
            public String render(StatementException exc, StatementContext ctx) {
                return exc.getShortMessage();
            }
        },
        /**
         * Include bound parameters but not the SQL.
         */
        PARAMETERS {
            @Override
            public String render(StatementException exc, StatementContext ctx) {
                return String.format("%s [arguments:%s]",
                            exc.getShortMessage(),
                            ctx.getBinding());
            }
        },
        /**
         * Include a length-limited SQL statement and parameter information.
         */
        SHORT_STATEMENT {
            @Override
            public String render(StatementException exc, StatementContext ctx) {
                final int limit = ctx.getConfig(StatementExceptions.class).getLengthLimit();
                return String.format("%s [statement:\"%s\", arguments:%s]",
                            exc.getShortMessage(),
                            limit(ctx.getRenderedSql(), limit),
                            limit(ctx.getBinding().toString(), limit));
            }

        },
        /**
         * Include all detail.
         */
        DETAIL {
            @Override
            public String render(StatementException exc, StatementContext ctx) {
                return String.format("%s [statement:\"%s\", rewritten:\"%s\", parsed:\"%s\", arguments:%s]",
                            exc.getShortMessage(),
                            ctx.getRawSql(),
                            ctx.getRenderedSql(),
                            ctx.getParsedSql(),
                            ctx.getBinding());
            }
        };

        @Override
        public String apply(StatementException exc) {
            final StatementContext ctx = exc.getStatementContext();
            if (ctx == null) {
                return NONE.render(exc, null);
            }
            return render(exc, ctx);
        }

        protected abstract String render(StatementException exc, StatementContext ctx);
    }

    protected static String limit(String s, int len) {
        if (s == null) {
            return null;
        }
        String truncated = s.substring(0, Math.min(len, s.length()));
        boolean isTruncated = len < s.length();
        if (isTruncated) {
            truncated += "[...]";
        }
        return truncated;
    }
}
