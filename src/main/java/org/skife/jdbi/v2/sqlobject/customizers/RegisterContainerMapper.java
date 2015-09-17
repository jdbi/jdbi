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
package org.skife.jdbi.v2.sqlobject.customizers;

import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;
import org.skife.jdbi.v2.tweak.ContainerFactory;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SqlStatementCustomizingAnnotation(RegisterContainerMapper.Factory.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterContainerMapper
{
    Class<? extends ContainerFactory>[] value();

    public static class Factory implements SqlStatementCustomizerFactory
    {

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            return new MyCustomizer((RegisterContainerMapper) annotation);
        }

        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            return new MyCustomizer((RegisterContainerMapper) annotation);
        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg)
        {
            throw new UnsupportedOperationException("Not Yet Implemented!");
        }
    }

    static class MyCustomizer implements SqlStatementCustomizer
    {
        private final List<ContainerFactory> factory;

        MyCustomizer(RegisterContainerMapper annotation)
        {
            List<ContainerFactory> ls = new ArrayList<ContainerFactory>();
            try {
                for (Class<? extends ContainerFactory> type : annotation.value()) {
                    ls.add(type.newInstance());
                }
            }
            catch (Exception e) {
                throw new IllegalStateException("Unable to instantiate container factory", e);
            }
            this.factory = ls;
        }

        @Override
        public void apply(SQLStatement q) throws SQLException
        {
            if (q instanceof Query) {
                Query query = (Query) q;
                for (ContainerFactory containerFactory : factory) {
                    query.registerContainerFactory(containerFactory);
                }
            }

        }
    }
}
