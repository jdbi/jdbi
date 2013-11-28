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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bind a {@code Map<String, Object>}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@BindingAnnotation(BindMapFactory.class)
public @interface BindMap
{
    /**
     * The list of allowed map keys to bind.
     * If not specified, to binds all provided {@code Map} entries.  Any missing parameters will cause an exception.
     * If specified, binds all provided keys.  Missing entries are bound with a SQL {@code NULL}.
     */
    String[] value() default {};

    /**
     * If specified, key {@code key} will be bound as {@code prefix.key}.
     */
    String prefix() default BindBean.BARE_BINDING;

    /**
     * Specify key handling.
     * If false, {@code Map} keys must be strings, or an exception is thrown.
     * If true, any object may be the key, and it will be converted with {@link Object#toString()}.
     */
    boolean implicitKeyStringConversion() default false;
}
