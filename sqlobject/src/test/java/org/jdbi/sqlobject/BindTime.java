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
package org.jdbi.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;

import org.jdbi.core.statement.StatementContext;
import org.jdbi.core.statement.StatementCustomizer;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.sqlobject.customizer.SqlStatementCustomizingAnnotation;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(BindTime.Factory.class)
public @interface BindTime {
    class Factory implements SqlStatementCustomizerFactory {
        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType) {
            // A configure-phase customizer registers a statement customizer that binds a fresh value
            // per execution, reading the clock from the execution's configuration.
            return stmt -> stmt.addCustomizer(new StatementCustomizer() {
                @Override
                public void beforeBinding(PreparedStatement preparedStatement, StatementContext ctx) {
                    ctx.getBinding().addNamed("now", OffsetDateTime.now(ctx.getConfig().get(BindTimeConfig.class).getClock()));
                }
            });
        }
    }
}
