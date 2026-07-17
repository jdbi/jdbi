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
package org.jdbi.sqlobject.customizer.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.function.Function;

import org.jdbi.core.statement.StatementContext;
import org.jdbi.core.statement.StatementCustomizer;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.sqlobject.customizer.Timestamped;
import org.jdbi.sqlobject.customizer.TimestampedConfig;

public class TimestampedFactory implements SqlStatementCustomizerFactory {

    private static Function<ZoneId, Clock> timeSource = Clock::system;

    @Override
    public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
        final String parameterName = ((Timestamped) annotation).value();

        // Configure-phase customizer: the timezone is read once (invariant), but a fresh timestamp
        // must be bound on every execution, so register a statement customizer that binds it at
        // binding time rather than binding a value now.
        return stmt -> {
            final ZoneId zone = stmt.getConfig().get(TimestampedConfig.class).getTimezone();
            stmt.addCustomizer(new StatementCustomizer() {
                @Override
                public void beforeBinding(PreparedStatement preparedStatement, StatementContext ctx) {
                    ctx.getBinding().addNamed(parameterName, OffsetDateTime.now(timeSource.apply(zone)));
                }
            });
        };
    }

    /**
     * for testing purposes only
     */
    static void setTimeSource(Function<ZoneId, Clock> timeSource) {
        TimestampedFactory.timeSource = timeSource;
    }
}
