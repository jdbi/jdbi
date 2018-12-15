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
package org.jdbi.v3.jackson2;

import org.jdbi.v3.json.TestJsonPlugin;
import org.jdbi.v3.postgres.PostgresDbRule;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.Rule;

public class TestJackson2Plugin extends TestJsonPlugin {
    @Rule
    public JdbiRule db = PostgresDbRule.rule();

    @Before
    public void before() {
        setJdbi(db.getJdbi().installPlugin(new Jackson2Plugin()));
    }

//    public static class Whozit {
//        private final String food;
//        private final int bitcoins;
//
//        @JsonCreator
//        public Whozit(@JsonProperty("food") String food, @JsonProperty("bitcoins") int bitcoins) {
//            this.food = food;
//            this.bitcoins = bitcoins;
//        }
//
//        public String getFood() {
//            return food;
//        }
//
//        public int getBitcoins() {
//            return bitcoins;
//        }
//    }
//
//    interface JsonWhozitDao {
//        @SqlUpdate("insert into whozits (whozit) values(?)")
//        int insert(@Json Whozit value);
//
//        @SqlQuery("select whozit from whozits")
//        @Json
//        List<Whozit> select();
//    }
//
//    public static class Beany {
//        private int id;
//        private Nested1 nested1;
//        private Nested2 nested2;
//
//        public Beany() {}
//        Beany(int id, int a, String b) {
//            this.id = id;
//            this.nested1 = new Nested1(a);
//            this.nested2 = new Nested2(b);
//        }
//
//        public int getId() {
//            return id;
//        }
//
//        public void setId(int id) {
//            this.id = id;
//        }
//
//        @Json
//        public Nested1 getNested1() {
//            return nested1;
//        }
//
//        public void setNested1(Nested1 nested1) {
//            this.nested1 = nested1;
//        }
//
//        @Json
//        public Nested2 getNested2() {
//            return nested2;
//        }
//
//        public void setNested2(Nested2 nested2) {
//            this.nested2 = nested2;
//        }
//    }
//
//    public static class Nested1 {
//        private final int a;
//
//        @JsonCreator
//        public Nested1(@JsonProperty("a") int a) {
//            this.a = a;
//        }
//
//        public int getA() {
//            return a;
//        }
//    }
//
//    public static class Nested2 {
//        private final String b;
//
//        @JsonCreator
//        public Nested2(@JsonProperty("b") String b) {
//            this.b = b;
//        }
//
//        public String getB() {
//            return b;
//        }
//    }
}
