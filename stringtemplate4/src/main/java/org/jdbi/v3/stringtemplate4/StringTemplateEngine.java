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
package org.jdbi.v3.stringtemplate4;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.TemplateEngine;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.misc.STMessage;

/**
 * Rewrites a StringTemplate template, using the attributes on the {@link StatementContext} as template parameters.
 * For configuration, see {@link StringTemplates}.
 */
public class StringTemplateEngine implements TemplateEngine.Parsing {
    /** Installed on an idle pooled prototype so it holds no execution context between renders. */
    private static final STErrorListener IDLE_LISTENER = new IdleListener();

    /** Non-cached render, for direct callers; the core uses {@link #parse(String, ConfigRegistry)}. */
    @Override
    public String render(String sql, StatementContext ctx) {
        return renderInstance(compile(sql, ctx), ctx);
    }

    /**
     * Caches compilation. StringTemplate is expensive to compile, and its {@link STGroup} and {@link ST} are
     * not thread-safe, so a compiled template must not be rendered by two threads at once. Compiled prototypes
     * are pooled rather than bound to a thread, so compilation is reused across platform and virtual threads
     * alike: a render checks a prototype out of the pool (compiling one only if the pool is empty), renders a
     * copy, and returns the prototype. Rendering is fast, non-blocking, and CPU-bound, so no more prototypes
     * are checked out at once than there are threads actually rendering; the pool self-bounds without a
     * capacity limit, holding its high-water mark of concurrent renders for the life of the cache entry.
     */
    @Override
    public Optional<Function<StatementContext, String>> parse(String sql, ConfigRegistry config) {
        final Queue<ST> pool = new ConcurrentLinkedQueue<>();
        return Optional.of(ctx -> {
            ST proto = pool.poll();
            if (proto == null) {
                proto = compile(sql, ctx);
            }
            // The prototype is checked out exclusively, so its group is not shared while in use.
            final STGroup group = proto.groupThatCreatedThisInstance;
            try {
                group.setListener(new ErrorListener(ctx));
                return renderInstance(new ST(proto), ctx);
            } finally {
                // Drop the execution context so an idle pooled prototype retains no StatementContext.
                group.setListener(IDLE_LISTENER);
                pool.offer(proto);
            }
        });
    }

    private static ST compile(String sql, StatementContext ctx) {
        STGroup group = new STGroup();
        group.setListener(new ErrorListener(ctx));
        return new ST(group, sql);
    }

    private static String renderInstance(ST template, StatementContext ctx) {
        ctx.getAttributes().forEach(template::add);
        return template.render();
    }

    static class ErrorListener implements STErrorListener {
        private final StatementContext ctx;

        ErrorListener(StatementContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void compileTimeError(STMessage msg) {
            throw new UnableToCreateStatementException("Compiling StringTemplate failed: " + msg, msg.cause, ctx);
        }

        @Override
        public void runTimeError(STMessage msg) {
            switch (msg.error) {
                case NO_SUCH_PROPERTY:
                    break;
                case NO_SUCH_ATTRIBUTE:
                    if (!ctx.getConfig(StringTemplates.class).isFailOnMissingAttribute()) {
                        break;
                    }
                // fallthrough
                default:
                    throw new UnableToExecuteStatementException("Executing StringTemplate failed: " + msg, msg.cause, ctx);
            }
        }

        @Override
        public void IOError(STMessage msg) {
            runTimeError(msg);
        }

        @Override
        public void internalError(STMessage msg) {
            runTimeError(msg);
        }
    }

    /** No-op listener for a pooled prototype at rest; a render always installs an {@link ErrorListener} first. */
    private static final class IdleListener implements STErrorListener {
        @Override
        public void compileTimeError(STMessage msg) {}

        @Override
        public void runTimeError(STMessage msg) {}

        @Override
        public void IOError(STMessage msg) {}

        @Override
        public void internalError(STMessage msg) {}
    }
}
