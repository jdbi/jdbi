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
package org.jdbi.v3.sqlobject.statement;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.result.ResultSetIterable;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;


/**
 * Used to specify a polymorphic return type parameter on a query method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@SqlStatementCustomizingAnnotation(MapTo.Factory.class)
public @interface MapTo {
    class Factory implements SqlStatementCustomizerFactory {
        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation, Class<?> sqlObjectType, Method method, Parameter param, int index, Object arg) {
            if (arg instanceof GenericType) {
                arg = ((GenericType<?>) arg).getType();
            }
            if (! (arg instanceof Type)) {
                throw new UnsupportedOperationException("@MapTo must take a Type, got a " + arg.getClass().getName());
            }
            return s -> {
                ResultReturner returner = ResultReturner.forMethod(sqlObjectType, method);
                s.getConfig(SqlObjectStatementConfiguration.class).setReturner(
                        () -> returner.result(((ResultSetIterable) s).mapTo((Type) arg)));
            };
        }
    }
}
