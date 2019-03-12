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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.json.AbstractJsonMapperTest;
import org.jdbi.v3.json.Json;
import org.jdbi.v3.postgres.PostgresDbRule;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestJackson2Plugin extends AbstractJsonMapperTest {

    @Rule
    public JdbiRule db = PostgresDbRule.rule();

    private Handle h;

    @Before
    public void before() {
        jdbi = db.getJdbi().installPlugin(new Jackson2Plugin());
        jdbi.getConfig(Jackson2Config.class).setMapper(new ObjectMapper().registerModule(new ParameterNamesModule()));
        h = jdbi.open();
    }

    @After
    public void close() {
        if (h != null) {
            h.close();
        }
    }

    @Test
    public void testGenericPolymorphicType() {
        ContainerDao dao = h.attach(ContainerDao.class);

        dao.table();

        Container<Contained> c1 = new Container<>();
        c1.setContained(new A());

        dao.insert(c1);

        assertThat(dao.get().getContained()).isInstanceOf(A.class);
    }

    private interface ContainerDao {
        @SqlUpdate("create table json(contained varchar)")
        void table();

        @SqlUpdate("insert into json(contained) values(:json)")
        void insert(@Bind("json") @Json Container<Contained> json);

        @SqlQuery("select * from json limit 1")
        @Json
        Container<Contained> get();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public interface Contained {}

    public static class A implements Contained {}

    public static class B implements Contained {}

    public static class Container<T> {

        private T contained;

        public T getContained() {
            return contained;
        }

        public void setContained(T contained) {
            this.contained = contained;
        }
    }

    private final QualifiedType<ViewTest> viewJsonType = QualifiedType.of(ViewTest.class).with(Json.class);
    private final ViewTest viewValue = new ViewTest().setA(42).setB(24);

    @Test
    public void testNoView() {
        assertThat(h.createQuery("select :vt::json ->> 'a' union all select :vt::json ->> 'b'")
                .bindByType("vt", viewValue, viewJsonType)
                .mapTo(int.class)
                .list())
            .containsExactly(42, 24);
    }

    @Test
    public void testSerializationView() {
        h.getConfig(Jackson2Config.class).setSerializationView(ViewTest.ViewB.class);
        assertThat(h.createQuery("select :vt::json ->> 'a'")
                .bindByType("vt", viewValue, viewJsonType)
                .mapTo(Integer.class)
                .findOnly())
            .isNull();
    }

    @Test
    public void testDeserializationView() {
        h.getConfig(Jackson2Config.class).setDeserializationView(ViewTest.ViewA.class);
        assertThat(h.createQuery("select '{\"a\":42,\"b\":43}'::json")
                .mapTo(viewJsonType)
                .findOnly())
            .extracting(ViewTest::getA, ViewTest::getB)
            .containsExactly(42, 0);
    }

    public static class ViewTest {
        public interface ViewA {}
        public interface ViewB {}

        private int a, b;

        @JsonView(ViewA.class)
        public int getA() {
            return a;
        }

        @JsonView(ViewA.class)
        public ViewTest setA(int a) {
            this.a = a;
            return this;
        }

        @JsonView(ViewB.class)
        public int getB() {
            return b;
        }

        @JsonView(ViewB.class)
        public ViewTest setB(int b) {
            this.b = b;
            return this;
        }
    }
}
