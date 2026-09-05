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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.TemplateEngine;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.compiler.CompiledST;
import org.stringtemplate.v4.misc.STMessage;

/**
 * Rewrites a StringTemplate template, using the attributes on the {@link StatementContext} as template parameters.
 * For configuration, see {@link StringTemplates}.
 */
public class StringTemplateEngine implements TemplateEngine.Parsing {
    /** Installed on an idle pooled prototype so it holds no execution context between renders. */
    private static final STErrorListener IDLE_LISTENER = new IdleListener();

    /**
     * Caps how many compiled prototypes one cache entry retains. Rendering is normally CPU-bound, so
     * concurrency rarely exceeds the processor count; a blocking attribute renderer (especially on virtual
     * threads) can, and then a render past the cap recompiles instead of growing the pool forever.
     */
    private static final int POOL_CAPACITY = 2 * Runtime.getRuntime().availableProcessors();

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
     * copy, and returns the prototype. The pool holds at most {@link #POOL_CAPACITY} prototypes; a render that
     * finds it empty compiles its own, and a return that finds it full discards.
     *
     * <p>The cached function bypasses {@link #render(String, StatementContext)}. A subclass that overrides
     * render() must also override this method so the two agree: either return {@link Optional#empty()} to
     * keep the core on the render() path, or return a function with the subclass's semantics.
     */
    @Override
    public Optional<Function<StatementContext, String>> parse(String sql, ConfigRegistry config) {
        final Queue<ST> pool = new LinkedBlockingQueue<>(POOL_CAPACITY);
        return Optional.of(ctx -> {
            ST proto = pool.poll();
            if (proto == null) {
                proto = compile(sql, ctx);
            }
            // The prototype is checked out exclusively, so its group is not shared while in use.
            final STGroup group = proto.groupThatCreatedThisInstance;
            try {
                group.setListener(new ErrorListener(ctx));
                // The copy must finish rendering before the prototype returns to the pool: ST4's
                // CompiledST.clone() hands the copy the formalArguments map the prototype held and gives the
                // prototype a fresh one, so an in-progress render would share that map with the next checkout.
                return renderInstance(new ST(proto), ctx);
            } finally {
                // Drop the execution context so an idle pooled prototype retains no StatementContext.
                group.setListener(IDLE_LISTENER);
                pool.offer(proto);
            }
        });
    }

    /**
     * The engine is stateless, so all instances of the same class are interchangeable. Equality by class lets
     * the core statement cache reuse compiled templates across instances, e.g. one created per statement or
     * by each {@code @UseStringTemplateEngine} annotation. A stateful subclass must override equals and
     * hashCode to keep differently-configured instances apart in the cache.
     */
    @Override
    public boolean equals(Object obj) {
        return obj != null && getClass() == obj.getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    private static ST compile(String sql, StatementContext ctx) {
        STGroup group = new StatementGroup();
        group.setListener(new ErrorListener(ctx));
        return new ST(group, sql);
    }

    private static String renderInstance(ST template, StatementContext ctx) {
        ctx.getAttributes().forEach(template::add);
        return template.render();
    }

    /**
     * STGroup that retains no negative lookup results. {@link STGroup#lookupTemplate} caches a not-found
     * marker per name, and a dynamic include such as {@code <(name)()>} derives names from attribute values,
     * so a pooled group's marker map would grow by one entry per distinct value for the life of the pool.
     */
    static final class StatementGroup extends STGroup {
        @Override
        public CompiledST lookupTemplate(String name) {
            CompiledST code = super.lookupTemplate(name);
            if (code == null) {
                templates.remove(name.charAt(0) == '/' ? name : "/" + name);
            }
            return code;
        }
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

    /**
     * Listener for a pooled prototype at rest. A render installs an {@link ErrorListener} before it touches the
     * group, so a callback here means a template executed outside a checkout, which is a bug in the caller.
     */
    private static final class IdleListener implements STErrorListener {
        @Override
        public void compileTimeError(STMessage msg) {
            idle(msg);
        }

        @Override
        public void runTimeError(STMessage msg) {
            idle(msg);
        }

        @Override
        public void IOError(STMessage msg) {
            idle(msg);
        }

        @Override
        public void internalError(STMessage msg) {
            idle(msg);
        }

        private static void idle(STMessage msg) {
            throw new IllegalStateException("StringTemplate reported an error on an idle pooled template: " + msg, msg.cause);
        }
    }
}
