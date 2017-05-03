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
package org.jdbi.v3.sqlobject.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.sqlobject.config.internal.RegisterColumnMapperImpl;

/**
 * Used to register a column mapper with either a sql object type or for a specific method.
 */
@ConfiguringAnnotation(RegisterColumnMapperImpl.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RegisterColumnMapper
{
    /**
     * The column mapper classes to register
     * @return one or more column mapper classes
     */
    Class<? extends ColumnMapper<?>>[] value();
}
