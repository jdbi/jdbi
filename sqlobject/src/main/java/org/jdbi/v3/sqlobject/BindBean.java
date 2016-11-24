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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds the properties of a JavaBean to a SQL statement.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@BindingAnnotation(BindBean.Factory.class)
public @interface BindBean
{
    /**
     * Prefix to apply to each bean property name. If specified, properties will be bound as
     * {@code prefix.propertyName}.
     */
    String value() default "";

    class Factory implements BinderFactory<BindBean, Object> {
        @Override
        public Binder<BindBean, Object> build(BindBean annotation) {
            return (statement, param, index, bind, bean) -> {
                String prefix = bind.value();
                if (prefix.isEmpty()) {
                    statement.bindBean(bean);
                }
                else {
                    statement.bindBean(prefix, bean);
                }
            };
        }
    }
}
