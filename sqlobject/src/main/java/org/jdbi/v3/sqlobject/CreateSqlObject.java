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

import org.jdbi.v3.sqlobject.internal.CreateSqlObjectHandler;

/**
 * Use this annotation on a sql object method to create a new sql object with the same underlying handle as the sql
 * object the method is invoked on. Not supported with on-demand SQL objects.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@SqlOperation(CreateSqlObjectHandler.class)
public @interface CreateSqlObject {}
