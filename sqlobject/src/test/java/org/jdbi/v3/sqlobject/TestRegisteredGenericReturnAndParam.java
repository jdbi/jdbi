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
package org.jdbi.v3.sqlobject;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Rule;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRegisteredGenericReturnAndParam
{
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testRegisterGenericRowMapperAnnotationWorks() throws Exception
    {
        testFoodToppingRestrictions(new GyroProvider(), 1);
        testFoodToppingRestrictions(new BurritoProvider(), 2);
    }

    private <T, R, P extends FoodProvider<T, R, ? extends Food<T, R>>> void testFoodToppingRestrictions(P provider, int id)
    {
        Food<T, R> food = dbRule.getJdbi().onDemand(provider.getDao());
        T topping = provider.getTopping();
        R restriction = provider.getRestriction();
        food.insertTopping(id, topping);
        List<Topping<T>> toppings = food.getToppings(id, restriction);
        assertThat(toppings.size()).isEqualTo(1);
        assertThat(toppings.get(0).value).isEqualTo(topping);
    }

    public interface FoodProvider<T, R, DAO extends Food<T, R>>
    {
        Class<DAO> getDao();
        T getTopping();
        R getRestriction();
    }

    public class GyroProvider implements FoodProvider<String, String, Gyro>
    {
        @Override
        public Class<Gyro> getDao()
        {
            return Gyro.class;
        }

        @Override
        public String getTopping()
        {
            return "yogurt";
        }

        @Override
        public String getRestriction()
        {
            return "vegetarian";
        }
    }

    public class BurritoProvider implements FoodProvider<String, Integer, Burrito>
    {
        @Override
        public Class<Burrito> getDao()
        {
            return Burrito.class;
        }

        @Override
        public String getTopping()
        {
            return "hot sauce";
        }

        @Override
        public Integer getRestriction()
        {
            return 3;
        }
    }

    @RegisterRowMapper(StringToppingMapper.class)
    public interface Gyro extends Food<String, String>
    {
        @SqlQuery("select id, name from something where id = :id and char_length(:str) > 5")
        @Override
        List<Topping<String>> getToppings(@Bind("id") int id, @Bind("str") String restrictions);
    }

    @RegisterRowMapper(StringToppingMapper.class)
    public interface Burrito extends Food<String, Integer>
    {
        @SqlQuery("select id, name from something where id = :id and :int + 1 > 0")
        @Override
        List<Topping<String>> getToppings(@Bind("id") int id, @Bind("int") Integer restrictions);
    }

    public interface Food<T, R>
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insertTopping(@Bind("id") int id, @Bind("name") T name);

        List<Topping<T>> getToppings(int id, R restrictions);
    }

    public static class Topping<T>
    {
        public T value;
        public Topping(T value) {
            this.value = value;
        }
    }

    public static class StringToppingMapper implements RowMapper<Topping<String>>
    {
        @Override
        public Topping<String> map(ResultSet r, StatementContext ctx) throws SQLException
        {
            return new Topping<>(r.getString("name"));
        }
    }
}
