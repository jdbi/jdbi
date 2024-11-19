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
package org.jdbi.v3.core.internal;

import java.util.Optional;

import jdk.jfr.FlightRecorder;
import org.jdbi.v3.core.statement.JdbiStatementEvent;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.StatementContext;

public final class JfrSupport {
    private JfrSupport() {
        throw new AssertionError("JfrSupport can not be instantiated");
    }

    public static final boolean PRESENT = ModuleLayer.boot().findModule("jdk.jfr").isPresent();

    public static void initJdbiEvent() {
        if (PRESENT) {
//            Class<?> fr = Class.forName("jdk.jfr.FlightRecorder");
//            Method m = fr.getMethod("register", Class.class);
//            m.invoke(null, JdbiStatementEvent.class);
            FlightRecorder.register(JdbiStatementEvent.class);
        }
    }

    public static Optional<JfrWrapper> beginEvent() {
        if (!PRESENT) {
            return Optional.empty();
        }

        return Optional.of(new JfrWrapper());
    }

    public static void attachJfrEvent(Optional<JfrWrapper> optWrapper, StatementContext ctx) {
        if (!PRESENT) {
            return;
        }
        optWrapper.ifPresent(wrapper -> {
            final var evt = wrapper.getEvent();
            if (evt.shouldCommit()) {
                evt.traceId = ctx.getTraceId();
                evt.type = ctx.describeJdbiStatementType();
                final var stmtConfig = ctx.getConfig(SqlStatements.class);
                final String renderedSql = ctx.getRenderedSql();
                if (renderedSql != null) {
                    evt.sql = renderedSql.substring(0,
                        Math.min(renderedSql.length(), stmtConfig.getJfrSqlMaxLength()));
                }
                evt.parameters = ctx.getBinding().describe(stmtConfig.getJfrParamMaxLength());
                ctx.addCleanable(() -> {
                    evt.rowsMapped = ctx.getMappedRows();
                    evt.commit();
                });
            }
        });
    }

    public static final class JfrWrapper {
        private final JdbiStatementEvent evt = new JdbiStatementEvent();

        private JfrWrapper() {
            evt.begin();
        }

        public JdbiStatementEvent getEvent() {
            return evt;
        }
    }
}
