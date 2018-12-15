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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jdbi.v3.json.Json;
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

    @Override
    protected Class<? extends BaseBeany> getBeanyClass() {
        return JacksonBeany.class;
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
    public static class JacksonBeany extends BaseBeany {
        // jackson apparently determines mapping based on fields, not methods, so we have to completely rewrite the parent class...
        private JacksonNested1 nested1;
        private JacksonNested2 nested2;

        public JacksonBeany() {}

        @Json
        @Override
        public JacksonNested1 getNested1() {
            return nested1;
        }

        public void setNested1(JacksonNested1 nested1) {
            this.nested1 = nested1;
        }

        @Json
        @Override
        public JacksonNested2 getNested2() {
            return nested2;
        }

        public void setNested2(JacksonNested2 nested2) {
            this.nested2 = nested2;
        }
    }

    public static class JacksonNested1 extends BaseNested1 {
        @JsonCreator
        public JacksonNested1(@JsonProperty("a") int a) {
            super(a);
        }
    }

    public static class JacksonNested2 extends BaseNested2 {
        @JsonCreator
        public JacksonNested2(@JsonProperty("b") String b) {
            super(b);
        }
    }
}
