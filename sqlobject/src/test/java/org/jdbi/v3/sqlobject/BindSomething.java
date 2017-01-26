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
package org.jdbi.v3.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(BindSomething.Factory.class)
public @interface BindSomething
{
    String value();

    class Factory implements SqlStatementCustomizer {
        @Override
        public void customizeForParameter(SqlStatement<?> statement,
                                          Annotation annotation,
                                          Class<?> sqlObjectType,
                                          Method method,
                                          Parameter param,
                                          int index,
                                          Object arg) {
            BindSomething bind = (BindSomething) annotation;
            Something it = (Something) arg;
            statement.bind(bind.value() + ".id", it.getId());
            statement.bind(bind.value() + ".name", it.getName());
        }
    }
}
