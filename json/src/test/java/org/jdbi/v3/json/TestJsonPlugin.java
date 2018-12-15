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
    public void testJsonMapping() {
        jdbi.useHandle(h -> {
            h.execute("create table whozits (id serial primary key, whozit json not null)");
            final BaseJsonWhozitDao dao = h.attach(BaseJsonWhozitDao.class);
            dao.insert(new BaseWhozit("yams", 42));
            dao.insert(new BaseWhozit("apples", 24));

            assertThat(dao.select())
                .extracting("food", "bitcoins")
                .containsExactlyInAnyOrder(
                    new Tuple("yams", 42),
                    new Tuple("apples", 24));
        });
    }

    @Test
    public void testJsonNested() {
        jdbi.useHandle(h -> {
            h.execute("create table beany (id serial primary key, nested1 json, nested2 json)");
            assertThat(h.createUpdate("insert into beany(id, nested1, nested2) values (:id, :nested1, :nested2)")
                .bindBean(new BaseBeany(42, 64, "quux"))
                .execute()).isEqualTo(1);

            BaseBeany beany = h.createQuery("select * from beany")
                .mapToBean(BaseBeany.class)
                .findOnly();

            assertThat(beany.getId()).isEqualTo(42);
            assertThat(beany.getNested1().getA()).isEqualTo(64);
            assertThat(beany.getNested2().getB()).isEqualTo("quux");
        });
    }

    @Test
    public void testNull() {
        jdbi.useHandle(h -> {
            h.execute("create table whozits (id serial primary key, whozit json)");
            final BaseJsonWhozitDao dao = h.attach(BaseJsonWhozitDao.class);
            dao.insert(null);
            assertThat(h.createQuery("select whozit from whozits")
                .mapTo(String.class)
                .findOnly())
                .isNull();
            assertThat(dao.select())
                .containsExactly((BaseWhozit) null);
        });
    }

    public static class BaseWhozit {
        private final String food;
        private final int bitcoins;

        public BaseWhozit(String food, int bitcoins) {
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

    interface BaseJsonWhozitDao {
        @SqlUpdate("insert into whozits (whozit) values(?)")
        int insert(@Json BaseWhozit value);

        @SqlQuery("select whozit from whozits")
        @Json
        List<BaseWhozit> select();
    }

    public static class BaseBeany {
        private int id;
        private BaseNested1 nested1;
        private BaseNested2 nested2;

        public BaseBeany() {}

        BaseBeany(int id, int a, String b) {
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
