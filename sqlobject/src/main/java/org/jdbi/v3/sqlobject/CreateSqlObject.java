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
import java.lang.reflect.Method;

import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.extension.HandleSupplier;

/**
 * Use this annotation on a sql object method to create a new sql object with the same underlying handle as the sql
 * object the method is invoked on. Not supported with on-demand SQL objects.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@SqlMethodAnnotation(CreateSqlObject.Impl.class)
public @interface CreateSqlObject
{
    class Impl implements Handler {
        private final Method method;

        public Impl(Method method) {
            this.method = method;
        }

        @Override
        public Object invoke(Object target, Object[] args, HandleSupplier handle) throws Exception {
            return handle.getConfig(Extensions.class)
                    .findFactory(SqlObjectFactory.class)
                    .orElseThrow(() -> new IllegalStateException("Can't locate SqlObject factory"))
                    .attach(method.getReturnType(), handle);
        }
    }
}
