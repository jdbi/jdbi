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
package org.jdbi.v3.sqlobject.config.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import edu.umd.cs.findbugs.annotations.Nullable;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.SimpleExtensionConfigurer;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.TemplateEngine;
import org.jdbi.v3.sqlobject.config.UseTemplateEngine;

public class UseTemplateEngineImpl extends SimpleExtensionConfigurer {

    private static final Class<?>[] TEMPLATE_ENGINE_PARAMETERS = {Class.class, Method.class};

    private final TemplateEngine templateEngine;

    public UseTemplateEngineImpl(Annotation annotation, Class<?> sqlObjectType, @Nullable Method method) {
        UseTemplateEngine useTemplateEngine = (UseTemplateEngine) annotation;
        Class<? extends TemplateEngine> engineClass = useTemplateEngine.value();
        this.templateEngine = JdbiClassUtils.findConstructorAndCreateInstance(engineClass, TEMPLATE_ENGINE_PARAMETERS, sqlObjectType, method);
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        config.get(SqlStatements.class).setTemplateEngine(templateEngine);
    }
}
