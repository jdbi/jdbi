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
package org.jdbi.v3.stringtemplate4.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.rewriter.StatementRewriter;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.stringtemplate4.StringTemplateStatementRewriter;
import org.jdbi.v3.stringtemplate4.UseStringTemplateStatementRewriter;

public class UseStringTemplateStatementRewriterImpl implements Configurer {
    @Override
    public void configureForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType) {
        UseStringTemplateStatementRewriter useStringTemplateStatementRewriter = (UseStringTemplateStatementRewriter) annotation;
        StatementRewriter delegate = createDelegate(useStringTemplateStatementRewriter.value());
        StatementRewriter rewriter = new StringTemplateStatementRewriter(delegate);
        registry.get(SqlStatements.class).setStatementRewriter(rewriter);
    }

    @Override
    public void configureForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method) {
        configureForType(registry, annotation, sqlObjectType);
    }

    private StatementRewriter createDelegate(Class<? extends StatementRewriter> type) {
        try {
            return type.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Error instantiating delegate statement rewriter: " + type, e);
        }
    }
}
