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
package org.skife.jdbi.v2.docs;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SomethingMapper;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.NamedArgumentFinder;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class TestBindExpression
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID());
        handle = dbi.open();
        handle.execute("create table something( id integer primary key, name varchar(100) )");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.close();
    }

    @RegisterMapper(SomethingMapper.class)
    public static interface DB
    {
        @SqlBatch("insert into something (id, name) values(:id, :name)")
        public void insert(@BindBean Something... things);

        @SqlQuery("select id, name from something where name = :breakfast.waffle.topping limit 1")
        public Something findByBreakfast(@BindRoot("breakfast") Breakfast b);
    }

    @Test
    public void testExpression() throws Exception
    {
        DB db = handle.attach(DB.class);
        db.insert(new Something(1, "syrup"), new Something(2, "whipped cream"));
        Something with_syrup = db.findByBreakfast(new Breakfast());
        assertThat(with_syrup, equalTo(new Something(1, "syrup")));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @SqlStatementCustomizingAnnotation(BindRoot.BindExpressionCustomizerFactory.class)
    public static @interface BindRoot
    {
        String value();

        public static class BindExpressionCustomizerFactory implements SqlStatementCustomizerFactory
        {
            @Override
            public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
            {
                throw new UnsupportedOperationException("Not Yet Implemented!");
            }

            @Override
            public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
            {
                throw new UnsupportedOperationException("Not Yet Implemented!");
            }

            @Override
            public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                             Class sqlObjectType,
                                                             Method method,
                                                             final Object root)
            {
                final String root_name = ((BindRoot) annotation).value();
                final JexlEngine engine = new JexlEngine();
                return new SqlStatementCustomizer()
                {
                    @Override
                    public void apply(final SQLStatement q) throws SQLException
                    {
                        q.bindNamedArgumentFinder(new NamedArgumentFinder()
                        {
                            @Override
                            public Argument find(String name)
                            {
                                Expression e = engine.createExpression(name);
                                final Object it = e.evaluate(new MapContext(ImmutableMap.of(root_name, root)));
                                if (it != null) {
                                    return q.getContext().getForeman().createArgument(it.getClass(), it, q.getContext());
                                }
                                else {
                                    return null;
                                }
                            }
                        });
                    }
                };
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
        private String topping = "syrup";

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
        assertThat((String) topping, equalTo("syrup"));
    }


}
