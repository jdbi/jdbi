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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.rewriter.SqlParser;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.sqlobject.config.UseSqlParser;

public class UseSqlParserImpl implements Configurer
{
    @Override
    public void configureForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method)
    {
        UseSqlParser anno = (UseSqlParser) annotation;
        try {
            final SqlParser parser = instantiate(anno.value(), sqlObjectType, method);
            registry.get(SqlStatements.class).setSqlParser(parser);
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void configureForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType)
    {
        UseSqlParser anno = (UseSqlParser) annotation;
        try {
            final SqlParser parser = instantiate(anno.value(), sqlObjectType, null);
            registry.get(SqlStatements.class).setSqlParser(parser);
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private SqlParser instantiate(Class<? extends SqlParser> value,
                                  Class<?> sqlObjectType,
                                  Method m) throws Exception
    {
        try {
            Constructor<? extends SqlParser> no_arg = value.getConstructor();
            return no_arg.newInstance();
        }
        catch (NoSuchMethodException e) {
            try {
                Constructor<? extends SqlParser> class_arg = value.getConstructor(Class.class);
                return class_arg.newInstance(sqlObjectType);
            }
            catch (NoSuchMethodException e1) {
                if (m != null) {
                    Constructor<? extends SqlParser> c_m_arg = value.getConstructor(Class.class,
                                                                                            Method.class);
                    return c_m_arg.newInstance(sqlObjectType, m);
                }
                throw new IllegalStateException("Unable to instantiate, no viable constructor " + value.getName());
            }
        }
    }
}
