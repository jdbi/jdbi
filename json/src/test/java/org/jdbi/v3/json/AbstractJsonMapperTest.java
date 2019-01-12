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
import java.util.Objects;

import org.assertj.core.groups.Tuple;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractJsonMapperTest {
    protected Jdbi jdbi;

    @Test
    public void testSqlObject() {
        jdbi.useHandle(h -> {
            h.execute("create table subjects (id serial primary key, subject json not null)");

            JsonDao dao = h.attach(JsonDao.class);

            dao.insert(new JsonBean("yams", 42));
            dao.insert(new JsonBean("apples", 24));

            assertThat(dao.select())
                .extracting("food", "bitcoins")
                .containsExactlyInAnyOrder(
                    new Tuple("yams", 42),
                    new Tuple("apples", 24));
        });
    }

    @Test
    public void testFluentApi() {
        jdbi.useHandle(h -> {
            h.execute("create table subjects (id serial primary key, subject json not null)");

            JsonBean in = new JsonBean("nom", 10);
            h.createUpdate("insert into subjects(id, subject) values(1, :bean)")
                .bindByType("bean", in, QualifiedType.of(JsonBean.class).with(Json.class))
                .execute();

            JsonBean out = h.createQuery("select subject from subjects")
                .mapTo(QualifiedType.of(JsonBean.class).with(Json.class))
                .findOnly();

            assertThat(out).isEqualTo(in);
        });
    }

    @Test
    public void testFluentApiWithNesting() {
        jdbi.useHandle(h -> {
            h.execute("create table bean (id serial primary key, nested1 json, nested2 json)");
            assertThat(h.createUpdate("insert into bean(id, nested1, nested2) values (:id, :nested1, :nested2)")
                .bindBean(new NestedJsonBean(42, 64, "quux"))
                .execute()).isEqualTo(1);

            NestedJsonBean beany = h.createQuery("select * from bean")
                .mapToBean(NestedJsonBean.class)
                .findOnly();

            assertThat(beany.getId()).isEqualTo(42);
            assertThat(beany.getNested1().getA()).isEqualTo(64);
            assertThat(beany.getNested2().getA()).isEqualTo("quux");
        });
    }

    @Test
    public void testNull() {
        jdbi.useHandle(h -> {
            h.execute("create table subjects (id serial primary key, subject json)");

            JsonDao dao = h.attach(JsonDao.class);

            dao.insert(null);

            assertThat(h.createQuery("select subject from subjects")
                .mapTo(String.class)
                .findOnly())
                .isNull();

            assertThat(dao.select())
                .containsExactly((JsonBean) null);
        });
    }

    public static class JsonBean {
        private final String food;
        private final int bitcoins;

        public JsonBean(String food, int bitcoins) {
            this.food = food;
            this.bitcoins = bitcoins;
        }

        public String getFood() {
            return food;
        }

        public int getBitcoins() {
            return bitcoins;
        }

        @Override
        public boolean equals(Object x) {
            JsonBean other = (JsonBean) x;
            return bitcoins == other.bitcoins
                && Objects.equals(food, other.food);
        }

        @Override
        public int hashCode() {
            return Objects.hash(food, bitcoins);
        }
    }

    public interface JsonDao {
        @SqlUpdate("insert into subjects (subject) values(?)")
        int insert(@Json JsonBean value);

        @SqlQuery("select subject from subjects")
        @Json
        List<JsonBean> select();
    }

    public static class NestedJsonBean {
        private int id;
        private Nested1 nested1;
        private Nested2 nested2;

        public NestedJsonBean() {}

        private NestedJsonBean(int id, int a, String b) {
            this.id = id;
            this.nested1 = new Nested1(a, "1");
            this.nested2 = new Nested2(b, 2);
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        @Json
        public Nested1 getNested1() {
            return nested1;
        }

        public void setNested1(Nested1 nested1) {
            this.nested1 = nested1;
        }

        @Json
        public Nested2 getNested2() {
            return nested2;
        }

        public void setNested2(Nested2 nested2) {
            this.nested2 = nested2;
        }
    }

    public static class Nested1 {
        private final int a;
        private final String b;

        public Nested1(int a, String b) {
            this.a = a;
            this.b = b;
        }

        public int getA() {
            return a;
        }

        public String getB() {
            return b;
        }
    }

    public static class Nested2 {
        private final String a;
        private final int b;

        public Nested2(String a, int b) {
            this.a = a;
            this.b = b;
        }

        public String getA() {
            return a;
        }

        public int getB() {
            return b;
        }
    }
}
