/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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
import java.lang.reflect.Parameter;

import org.jdbi.v3.SQLStatement;
import org.jdbi.v3.Something;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@BindingAnnotation(BindSomething.Factory.class)
public @interface BindSomething
{
    String value();

    static class Factory implements BinderFactory
    {
        @Override
        public Binder build(Annotation annotation)
        {
            return new Binder()
            {
                @Override
                public void bind(SQLStatement q, Parameter param, Annotation bind, Object arg)
                {
                    BindSomething bs = (BindSomething) bind;
                    Something it = (Something) arg;
                    q.bind(bs.value() + ".id", it.getId());
                    q.bind(bs.value() + ".name", it.getName());
                }
            };
        }
    }
}
