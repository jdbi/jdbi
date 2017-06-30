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
package org.jdbi.v3.stringtemplate4;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.sqlobject.config.ConfiguringAnnotation;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.stringtemplate4.internal.UseStringTemplateStatementRewriterImpl;

/**
 * Configures a SQL object class or method to rewrite SQL statements using StringTemplate. Method parameters annotated
 * with {@link Define @Define} are passed to the StringTemplate as template
 * attributes.
 */
@ConfiguringAnnotation(UseStringTemplateStatementRewriterImpl.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UseStringTemplateStatementRewriter {
}
