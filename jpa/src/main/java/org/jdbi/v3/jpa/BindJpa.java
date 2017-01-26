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
package org.jdbi.v3.jpa;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@SqlStatementCustomizingAnnotation(BindJpa.Factory.class)
public @interface BindJpa {
    String value() default "";

    class Factory implements SqlStatementCustomizerFactory {
        @Override
        public SqlStatementParameterCustomizer createForParameter(Annotation annotation,
                                                                  Class<?> sqlObjectType,
                                                                  Method method,
                                                                  Parameter param,
                                                                  int index) {
            BindJpa bind = (BindJpa) annotation;
            final String prefix;
            if (bind.value().isEmpty()) {
                prefix = "";
            } else {
                prefix = bind.value() + ".";
            }
            return (stmt, arg) -> {
                JpaClass<?> jpaClass = JpaClass.get(arg.getClass());
                for (JpaMember member : jpaClass.members()) {
                    stmt.bindByType(
                            prefix + member.getColumnName(),
                            readMember(arg, member),
                            member.getType());
                }
            };
        }

        private static Object readMember(Object entity, JpaMember member) {
            try {
                return member.read(entity);
            } catch (IllegalAccessException e) {
                String message = String.format(
                        "Unable to access property value for column %s",
                        member.getColumnName());
                throw new EntityMemberAccessException(message, e);
            } catch (InvocationTargetException e) {
                String message = String.format(
                        "Exception thrown in accessor method for column %s",
                        member.getColumnName());
                throw new EntityMemberAccessException(message, e);
            }
        }
    }
}
