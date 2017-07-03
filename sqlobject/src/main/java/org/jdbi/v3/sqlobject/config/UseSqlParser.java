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

import org.jdbi.v3.core.rewriter.SqlParser;
import org.jdbi.v3.sqlobject.config.internal.UseSqlParserImpl;

/**
 * Use the specified {@link SqlParser} class to parse SQL for the annotated SQL object class or method. The
 * given {@link SqlParser} class must have a public constructor with any of the following signatures:
 * <ul>
 * <li>RewriterClass() // no arguments</li>
 * <li>RewriterClass(Class)</li>
 * <li>RewriterClass(Class,Method)</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@ConfiguringAnnotation(UseSqlParserImpl.class)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface UseSqlParser
{
    /**
     * Specify the SqlParser class to use.
     * @return the SqlParser class to use.
     */
    Class<? extends SqlParser> value();
}
