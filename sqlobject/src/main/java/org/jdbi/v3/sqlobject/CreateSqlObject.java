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

import com.fasterxml.classmate.members.ResolvedMethod;

/**
 * Use this annotation on a sql object method to create a new sql object with the same underlying handle as the sql
 * object the method is invoked on. Not supported with on-demand SQL objects.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@SqlMethodAnnotation(CreateSqlObject.Factory.class)
public @interface CreateSqlObject
{
    class Factory implements HandlerFactory {
        @Override
        public Handler buildHandler(Class<?> sqlObjectType, ResolvedMethod method, SqlObject config) {
            return new CreateSqlObjectHandler(method.getRawMember().getReturnType(), config);
        }
    }
}
