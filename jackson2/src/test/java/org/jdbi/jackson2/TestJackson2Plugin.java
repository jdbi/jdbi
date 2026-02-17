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
package org.jdbi.jackson2;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.immutables.value.Value;
import org.jdbi.core.Handle;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.mapper.immutables.JdbiImmutables;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.json.AbstractJsonMapperTest;
import org.jdbi.json.Json;
import org.jdbi.postgres.PostgresPlugin;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.customizer.BindPojo;
import org.jdbi.sqlobject.statement.SqlBatch;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestJackson2Plugin extends AbstractJsonMapperTest {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    JdbiExtension pgExtension = JdbiExtension.postgres(pg)
        .withPlugins(new SqlObjectPlugin(), new PostgresPlugin(), new Jackson2Plugin())
        .withConfig(Jackson2Config.class, c -> c.setMapper(new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())))
        .withConfig(JdbiImmutables.class, i -> i.registerImmutable(JsonContainer.class));

    private Handle h;

    @BeforeEach
    public void before() {
        this.jdbi = pgExtension.getJdbi();
        this.h = pgExtension.openHandle();
    }

    @AfterEach
    public void after() {
        this.h.close();
    }

    @Test
    public void testGenericPolymorphicType() {
        final ContainerDao dao = h.attach(ContainerDao.class);

        dao.table();

        final Container<Contained> c1 = new Container<>();
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

    public static class A implements Contained, SomeTypeBound {}

    public static class B implements Contained {}

    public static class Container<T> {

        private T contained;

        public T getContained() {
            return contained;
        }

        public void setContained(final T contained) {
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
                .one())
            .isNull();
    }

    @Test
    public void testDeserializationView() {
        h.getConfig(Jackson2Config.class).setDeserializationView(ViewTest.ViewA.class);
        assertThat(h.createQuery("select '{\"a\":42,\"b\":43}'::json")
                .mapTo(viewJsonType)
                .one())
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
        public ViewTest setA(final int a) {
            this.a = a;
            return this;
        }

        @JsonView(ViewB.class)
        public int getB() {
            return b;
        }

        @JsonView(ViewB.class)
        public ViewTest setB(final int b) {
            this.b = b;
            return this;
        }
    }

    @Test
    public void testPolymorphicJsonContainerWithTypeBound() {
        final ExtendingDao dao = h.attach(ExtendingDao.class);

        dao.table();

        final JsonContainer<A> c1 = JsonContainer.<A>builder()
                .contained(new A())
                .build();

        dao.insertBatch(Collections.singletonList(c1));

        assertThat(dao.get().contained()).isInstanceOf(A.class);
    }

    @Value.Immutable
    @Value.Style(overshadowImplementation = true)
    @JsonSerialize(as = ImmutableJsonContainer.class)
    @JsonDeserialize(as = ImmutableJsonContainer.class)
    public interface JsonContainer<T extends SomeTypeBound> {
        @Json
        T contained();

        static <T extends SomeTypeBound> Builder<T> builder() {
            return new Builder<>();
        }
        class Builder<T extends SomeTypeBound> extends ImmutableJsonContainer.Builder<T> {}
    }

    interface SomeTypeBound {}

    interface GenericContainingDao<T extends SomeTypeBound> {
        @SqlUpdate("create table jsoncontainer(contained varchar)")
        void table();

        @SqlBatch("insert into jsoncontainer(contained) values(:contained)")
        void insertBatch(@BindPojo Collection<JsonContainer<T>> json);

        @SqlQuery("select contained from jsoncontainer limit 1")
        JsonContainer<T> get();
    }

    private interface ExtendingDao extends GenericContainingDao<A> {}

    @Test
    public void jsonbSet() {
        assertThat(
                h.createQuery("select jsonb_set('{}', '{val}', :val)")
                    .bindByType("val", "testing", QualifiedType.of(String.class).with(Json.class))
                    .mapTo(QualifiedType.of(new GenericType<Map<String, String>>() {}).with(Json.class))
                    .one())
            .isEqualTo(Collections.singletonMap("val", "testing"));
    }

    public interface PolyBase {}
    public static class PolySub implements PolyBase {
        public int getA() {
            return 42;
        }
    }

    @Test
    public void dynamicType() {
        assertThat(h.createQuery("select :val::varchar")
                .configure(Jackson2Config.class, jc -> jc.setUseStaticType(false))
                .bindByType("val", new PolySub(), QualifiedType.of(PolyBase.class).with(Json.class))
                .mapTo(String.class)
                .one())
            .isEqualTo("{\"a\":42}");
    }
}
