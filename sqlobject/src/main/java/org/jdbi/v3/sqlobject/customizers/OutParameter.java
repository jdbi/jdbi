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
package org.jdbi.v3.sqlobject.customizers;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.SQLException;

import org.jdbi.v3.core.Call;
import org.jdbi.v3.core.SqlStatement;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;

/**
 * Declare a named out parameter on an {@code @SqlCall} annotated method.
 * Note that you *must* include the parameter name in the SQL text to
 * ensure that the binding is activated, this is a limitation that
 * may be fixed at a future date.
 *
 * Example usage, using PostgreSQL call syntax:
 * <pre>
 * {@code
 *   handle.execute("CREATE FUNCTION set100(OUT outparam INT) AS $$ BEGIN outparam \\:= 100; END; $$ LANGUAGE plpgsql");
 *
 *   @SqlCall("{call myStoredProc(:outparam)}")
 *   @OutParameter(name="outparam", sqlType = Types.INTEGER)
 *   OutParameters callStoredProc();
 * }
 * </pre>
 */
@SqlStatementCustomizingAnnotation(OutParameter.Factory.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OutParameter {
    String name();
    int sqlType();

    class Factory implements SqlStatementCustomizerFactory {

        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, @SuppressWarnings("rawtypes") Class sqlObjectType) {
            throw new UnsupportedOperationException("Not allowed on Type");
        }

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, @SuppressWarnings("rawtypes") Class sqlObjectType, Method method) {
            final OutParameter outParam = (OutParameter) annotation;
            return new SqlStatementCustomizer() {
                @Override
                public void apply(@SuppressWarnings("rawtypes") SqlStatement q) throws SQLException {
                    ((Call) q).registerOutParameter(outParam.name(), outParam.sqlType());
                }
            };
        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation, @SuppressWarnings("rawtypes") Class sqlObjectType, Method method, final Object arg) {
            throw new UnsupportedOperationException("Not defined for parameter");
        }
    }
}
