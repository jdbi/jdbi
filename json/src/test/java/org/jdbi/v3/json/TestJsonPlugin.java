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
package org.jdbi.v3.json;

import java.util.List;
import org.assertj.core.groups.Tuple;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public abstract class TestJsonPlugin {
    private Jdbi jdbi;

    protected void setJdbi(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Test
    public void testSqlObject() {
        jdbi.useHandle(h -> {
            h.execute("create table subjects (id serial primary key, subject json not null)");

            BaseDao<BaseDaoSubject> dao = h.attach(getDaoClass());

            dao.insert(new BaseDaoSubject("yams", 42));
            dao.insert(new BaseDaoSubject("apples", 24));

            assertThat(dao.select())
                .extracting("food", "bitcoins")
                .containsExactlyInAnyOrder(
                    new Tuple("yams", 42),
                    new Tuple("apples", 24));
        });
    }

    protected abstract Class<? extends BaseDao> getDaoClass();

    @Test
    public void testFluentApiWithNesting() {
        jdbi.useHandle(h -> {
            h.execute("create table bean (id serial primary key, nested1 json, nested2 json)");
            assertThat(h.createUpdate("insert into bean(id, nested1, nested2) values (:id, :nested1, :nested2)")
                .bindBean(new BaseBean(42, 64, "quux"))
                .execute()).isEqualTo(1);

            BaseBean beany = h.createQuery("select * from bean")
                .mapToBean(getBeanClass())
                .findOnly();

            assertThat(beany.getId()).isEqualTo(42);
            assertThat(beany.getNested1().getA()).isEqualTo(64);
            assertThat(beany.getNested2().getB()).isEqualTo("quux");
        });
    }

    protected abstract Class<? extends BaseBean> getBeanClass();

    @Test
    public void testNull() {
        jdbi.useHandle(h -> {
            h.execute("create table subjects (id serial primary key, subject json)");

            BaseDao<BaseDaoSubject> dao = h.attach(BaseDao.class);

            dao.insert(null);

            assertThat(h.createQuery("select subject from subjects")
                .mapTo(String.class)
                .findOnly())
                .isNull();

            assertThat(dao.select())
                .containsExactly((BaseDaoSubject) null);
        });
    }

    public static class BaseDaoSubject {
        private final String food;
        private final int bitcoins;

        public BaseDaoSubject(String food, int bitcoins) {
            this.food = food;
            this.bitcoins = bitcoins;
        }

        public String getFood() {
            return food;
        }

        public int getBitcoins() {
            return bitcoins;
        }
    }

    public interface BaseDao<T extends BaseDaoSubject> {
        @SqlUpdate("insert into subjects (subject) values(?)")
        int insert(@Json T value);

        @SqlQuery("select subject from subjects")
        @Json
        List<T> select();
    }

    public static class BaseBean {
        private int id;
        private BaseNested1 nested1;
        private BaseNested2 nested2;

        public BaseBean() {}

        private BaseBean(int id, int a, String b) {
            this.id = id;
            this.nested1 = new BaseNested1(a);
            this.nested2 = new BaseNested2(b);
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        @Json
        public BaseNested1 getNested1() {
            return nested1;
        }

        public void setNested1(BaseNested1 nested1) {
            this.nested1 = nested1;
        }

        @Json
        public BaseNested2 getNested2() {
            return nested2;
        }

        public void setNested2(BaseNested2 nested2) {
            this.nested2 = nested2;
        }
    }

    public static class BaseNested1 {
        private final int a;

        public BaseNested1(int a) {
            this.a = a;
        }

        public int getA() {
            return a;
        }
    }

    public static class BaseNested2 {
        private final String b;

        public BaseNested2(String b) {
            this.b = b;
        }

        public String getB() {
            return b;
        }
    }
}
