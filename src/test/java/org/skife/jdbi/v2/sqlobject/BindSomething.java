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
package org.skife.jdbi.v2.sqlobject;


import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.Something;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@BindingAnnotation(BindSomething.Factory.class)
public @interface BindSomething
{
    String value();

    static class Factory implements BinderFactory
    {
        public Binder build(Annotation annotation)
        {
            return new Binder()
            {
                public void bind(SQLStatement q, Annotation bind, Object arg)
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
