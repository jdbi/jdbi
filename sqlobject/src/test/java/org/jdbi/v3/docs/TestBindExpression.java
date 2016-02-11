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
package org.jdbi.v3.docs;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;
import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.BindBean;
import org.jdbi.v3.sqlobject.SomethingMapper;
import org.jdbi.v3.sqlobject.SqlBatch;
import org.jdbi.v3.sqlobject.SqlObjectBuilder;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.junit.Rule;
import org.junit.Test;

public class TestBindExpression
{
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @RegisterMapper(SomethingMapper.class)
    public interface DB
    {
        @SqlBatch("insert into something (id, name) values(:id, :name)")
        void insert(@BindBean Something... things);

        @SqlQuery("select id, name from something where name = :breakfast.waffle.topping limit 1")
        Something findByBreakfast(@BindRoot("breakfast") Breakfast b);
    }

    @Test
    public void testExpression() throws Exception
    {
        DB db = SqlObjectBuilder.attach(dbRule.getSharedHandle(), DB.class);
        db.insert(new Something(1, "syrup"), new Something(2, "whipped cream"));
        Something with_syrup = db.findByBreakfast(new Breakfast());
        assertThat(with_syrup, equalTo(new Something(1, "syrup")));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @SqlStatementCustomizingAnnotation(BindRoot.BindExpressionCustomizerFactory.class)
    public @interface BindRoot
    {
        String value();

        class BindExpressionCustomizerFactory implements SqlStatementCustomizerFactory
        {
            @Override
            public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                             Class<?> sqlObjectType,
                                                             Method method,
                                                             final Object root)
            {
                final String root_name = ((BindRoot) annotation).value();
                final JexlEngine engine = new JexlEngine();
                return q -> q.bindNamedArgumentFinder(name -> {
                    Expression e = engine.createExpression(name);
                    final Object it = e.evaluate(new MapContext(ImmutableMap.of(root_name, root)));
                    return it == null
                            ? Optional.empty()
                            : Optional.of((position, statement, ctx) -> statement.setObject(position, it));
                });
            }
        }
    }


    public static class Breakfast
    {
        private final Waffle waffle = new Waffle();

        public Waffle getWaffle()
        {
            return waffle;
        }
    }

    public static class Waffle
    {
        private final String topping = "syrup";

        public String getTopping()
        {
            return topping;
        }
    }


    @Test
    public void testJexl() throws Exception
    {
        JexlEngine engine = new JexlEngine();
        Object topping = engine.createExpression("breakfast.waffle.topping")
                               .evaluate(new MapContext(ImmutableMap.<String, Object>of("breakfast", new Breakfast())));
        assertThat(topping, instanceOf(String.class));
        assertThat(topping, equalTo("syrup"));
    }


}
