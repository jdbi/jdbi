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
package org.jdbi.v3.jpa;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.assertj.core.api.AbstractListAssert;
import org.jdbi.v3.core.qualifier.Reversed;
import org.jdbi.v3.core.qualifier.ReversedStringArgumentFactory;
import org.jdbi.v3.core.qualifier.ReversedStringMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapperFactory;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaTest {
    private static final String INSERT_BY_PROPERTY_NAME = "insert into something(id, name) values (:id, :name)";
    private static final String SELECT_BY_PROPERTY_NAME = "select id, name from something";

    private static final String INSERT_BY_ANNOTATION_NAME = "insert into something (id, name) values (:foo, :bar)";
    private static final String SELECT_BY_ANNOTATION_NAME = "select id as foo, name as bar from something";
    private static final String ID_ANNOTATION_NAME = "foo";
    private static final String NAME_ANNOTATION_NAME = "bar";

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    interface Thing {
        int getId();

        String getName();
    }

    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    private static class EntityThing implements Thing {
        private int id;
        private String name;
    }

    public interface EntityThingDao {
        @SqlUpdate(INSERT_BY_PROPERTY_NAME)
        void insert(@BindJpa EntityThing thing);

        @SqlQuery(SELECT_BY_PROPERTY_NAME)
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        List<EntityThing> list();
    }

    @Test
    public void testEntityNoColumnAnnotations() {
        EntityThing brian = new EntityThing(1, "Brian");
        EntityThing keith = new EntityThing(2, "Keith");

        EntityThingDao dao = dbRule.getSharedHandle().attach(EntityThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<EntityThing> rs = dao.list();

        assertThatThing(rs).containsOnlyOnce(brian, keith);
    }

    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    private static class FieldThing implements Thing {
        @Column
        private int id;
        @Column
        private String name;
    }

    public interface FieldThingDao {
        @SqlUpdate(INSERT_BY_PROPERTY_NAME)
        void insert(@BindJpa FieldThing thing);

        @SqlQuery(SELECT_BY_PROPERTY_NAME)
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        List<FieldThing> list();
    }

    @Test
    public void testField() {
        FieldThing brian = new FieldThing(1, "Brian");
        FieldThing keith = new FieldThing(2, "Keith");

        FieldThingDao dao = dbRule.getSharedHandle().attach(FieldThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<FieldThing> rs = dao.list();
        assertThatThing(rs).containsOnlyOnce(brian, keith);
    }

    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    private static class NamedFieldThing implements Thing {
        @Column(name = "foo")
        private int id;
        @Column(name = "bar")
        private String name;
    }

    public interface NamedFieldThingDao {
        @SqlUpdate(INSERT_BY_ANNOTATION_NAME)
        void insert(@BindJpa NamedFieldThing thing);

        @SqlQuery(SELECT_BY_ANNOTATION_NAME)
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        List<NamedFieldThing> list();
    }

    @Test
    public void testNamedField() {
        NamedFieldThing brian = new NamedFieldThing(1, "Brian");
        NamedFieldThing keith = new NamedFieldThing(2, "Keith");

        NamedFieldThingDao dao = dbRule.getSharedHandle().attach(NamedFieldThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<NamedFieldThing> rs = dao.list();

        assertThatThing(rs).containsOnlyOnce(brian, keith);
    }

    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    static class GetterThing implements Thing {
        private int id;
        private String name;

        @Column
        public int getId() {
            return id;
        }

        @Column
        public String getName() {
            return name;
        }
    }

    public interface GetterThingDao {
        @SqlUpdate(INSERT_BY_PROPERTY_NAME)
        void insert(@BindJpa GetterThing thing);

        @SqlQuery(SELECT_BY_PROPERTY_NAME)
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        List<GetterThing> list();
    }

    @Test
    public void testGetter() {
        GetterThing brian = new GetterThing(1, "Brian");
        GetterThing keith = new GetterThing(2, "Keith");

        GetterThingDao dao = dbRule.getSharedHandle().attach(GetterThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<GetterThing> rs = dao.list();

        assertThatThing(rs).containsOnlyOnce(brian, keith);
    }

    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    static class NamedGetterThing implements Thing {
        private int id;
        private String name;

        @Column(name = "foo")
        public int getId() {
            return id;
        }

        @Column(name = "bar")
        public String getName() {
            return name;
        }
    }

    public interface NamedGetterThingDao {
        @SqlUpdate(INSERT_BY_ANNOTATION_NAME)
        void insert(@BindJpa NamedGetterThing thing);

        @SqlQuery(SELECT_BY_ANNOTATION_NAME)
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        List<NamedGetterThing> list();
    }

    @Test
    public void testNamedGetter() {
        NamedGetterThing brian = new NamedGetterThing(1, "Brian");
        NamedGetterThing keith = new NamedGetterThing(2, "Keith");

        NamedGetterThingDao dao = dbRule.getSharedHandle().attach(NamedGetterThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<NamedGetterThing> rs = dao.list();

        assertThatThing(rs).containsOnlyOnce(brian, keith);
    }

    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    static class SetterThing implements Thing {
        private int id;
        private String name;

        @Column
        public void setId(int id) {
            this.id = id;
        }

        @Column
        public void setName(String name) {
            this.name = name;
        }
    }

    public interface SetterThingDao {
        @SqlUpdate(INSERT_BY_PROPERTY_NAME)
        void insert(@BindJpa SetterThing thing);

        @SqlQuery(SELECT_BY_PROPERTY_NAME)
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        List<SetterThing> list();
    }

    @Test
    public void testSetter() {
        SetterThing brian = new SetterThing(1, "Brian");
        SetterThing keith = new SetterThing(2, "Keith");

        SetterThingDao dao = dbRule.getSharedHandle().attach(SetterThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<SetterThing> rs = dao.list();

        assertThatThing(rs).containsOnlyOnce(brian, keith);
    }

    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    static class NamedSetterThing implements Thing {
        private int id;
        private String name;

        @Column(name = "foo")
        public void setId(int id) {
            this.id = id;
        }

        @Column(name = "bar")
        public void setName(String name) {
            this.name = name;
        }
    }

    public interface NamedSetterThingDao {
        @SqlUpdate(INSERT_BY_ANNOTATION_NAME)
        void insert(@BindJpa NamedSetterThing thing);

        @SqlQuery(SELECT_BY_ANNOTATION_NAME)
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        List<NamedSetterThing> list();
    }

    @Test
    public void testNamedSetter() {
        NamedSetterThing brian = new NamedSetterThing(1, "Brian");
        NamedSetterThing keith = new NamedSetterThing(2, "Keith");

        NamedSetterThingDao dao = dbRule.getSharedHandle().attach(NamedSetterThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<NamedSetterThing> rs = dao.list();
        assertThatThing(rs).containsOnlyOnce(brian, keith);
    }

    @MappedSuperclass
    @Getter
    @Setter
    static class MappedSuperclassThing {
        private int id;
    }

    @Entity
    @NoArgsConstructor
    @Getter
    @Setter
    static class ExtendsMappedSuperclassThing extends MappedSuperclassThing implements Thing {
        private String name;

        ExtendsMappedSuperclassThing(int id, String name) {
            setId(id);
            setName(name);
        }
    }

    public interface MappedSuperclassThingDao {
        @SqlUpdate(INSERT_BY_PROPERTY_NAME)
        void insert(@BindJpa ExtendsMappedSuperclassThing thing);

        @SqlQuery(SELECT_BY_PROPERTY_NAME)
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        List<ExtendsMappedSuperclassThing> list();
    }

    @Test
    public void testMappedSuperclass() {
        ExtendsMappedSuperclassThing brian = new ExtendsMappedSuperclassThing(1, "Brian");
        ExtendsMappedSuperclassThing keith = new ExtendsMappedSuperclassThing(2, "Keith");

        MappedSuperclassThingDao dao = dbRule.getSharedHandle().attach(MappedSuperclassThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<ExtendsMappedSuperclassThing> rs = dao.list();
        assertThatThing(rs).containsOnlyOnce(brian, keith);
    }

    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    static class AnnotationPriorityThing implements Thing {
        @Column(name = ID_ANNOTATION_NAME)
        private int id;
        private String name;

        @Column(name = "ignored")
        public int getId() {
            return id;
        }

        @Column(name = NAME_ANNOTATION_NAME)
        public String getName() {
            return name;
        }

        @Column(name = "ignored")
        public void setId(int id) {
            this.id = id;
        }

        @Column(name = "ignored")
        public void setName(String name) {
            this.name = name;
        }
    }

    public interface AnnotationPriorityThingDao {
        @SqlUpdate(INSERT_BY_ANNOTATION_NAME)
        void insert(@BindJpa AnnotationPriorityThing thing);

        @SqlQuery(SELECT_BY_ANNOTATION_NAME)
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        List<AnnotationPriorityThing> list();
    }

    @Test
    public void testAnnotationPriority() {
        // fields before getters before setters
        AnnotationPriorityThing brian = new AnnotationPriorityThing(1, "Brian");
        AnnotationPriorityThing keith = new AnnotationPriorityThing(2, "Keith");

        AnnotationPriorityThingDao dao = dbRule.getSharedHandle().attach(AnnotationPriorityThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<AnnotationPriorityThing> rs = dao.list();
        assertThatThing(rs).containsOnlyOnce(brian, keith);
    }

    public interface SuperfluousColumnDao {
        @SqlUpdate(INSERT_BY_PROPERTY_NAME)
        void insert(@BindJpa FieldThing thing);

        @SqlQuery("select id, name, 'Rob Schneider' as extra from something")
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        List<FieldThing> list();
    }

    @Test
    public void testMapWithSuperfluousColumn() {
        FieldThing brian = new FieldThing(1, "Brian");
        FieldThing keith = new FieldThing(2, "Keith");

        SuperfluousColumnDao dao = dbRule.getSharedHandle().attach(SuperfluousColumnDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<FieldThing> rs = dao.list();
        assertThatThing(rs).containsOnlyOnce(brian, keith);
    }

    public interface MissingColumnDao {
        @SqlUpdate("insert into something(id) values (:id)")
        void insert(@BindJpa FieldThing thing);

        @SqlQuery("select id from something")
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        List<FieldThing> list();
    }

    @Test
    public void testMissingColumn() {
        FieldThing brian = new FieldThing(1, "Brian");
        FieldThing keith = new FieldThing(2, "Keith");

        MissingColumnDao dao = dbRule.getSharedHandle().attach(MissingColumnDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<FieldThing> rs = dao.list();
        assertThatThing(rs).containsOnlyOnce(new FieldThing(1, null), new FieldThing(2, null));
    }

    @MappedSuperclass
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class OverriddenSuperclassThing implements Thing {
        @Column(name = "foo")
        private int id;
        @Column(name = "bar")
        private String name;
    }

    @Entity
    @NoArgsConstructor
    static class OverridingSubclassThing extends OverriddenSuperclassThing {
        OverridingSubclassThing(int id, String name) {
            super(id, name);
        }

        @Override
        @Column(name = "meow")
        public int getId() {
            return super.getId();
        }
    }

    public interface OverridingSubclassThingDao {
        @SqlUpdate("insert into something(id, name) values (:meow, :bar)")
        void insert(@BindJpa OverridingSubclassThing thing);

        @SqlQuery("select id as meow, name as bar from something")
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        List<OverridingSubclassThing> list();
    }

    @Test
    public void subclassAnnotationOverridesSuperclass() {
        // Annotated takes precedence over no annotation, even if annotated in superclass
        // Annotated member in subclass takes precedence over annotated member in superclass

        OverridingSubclassThing brian = new OverridingSubclassThing(1, "Brian");
        OverridingSubclassThing keith = new OverridingSubclassThing(2, "Keith");

        OverridingSubclassThingDao dao = dbRule.getSharedHandle().attach(OverridingSubclassThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<OverridingSubclassThing> rs = dao.list();

        assertThatThing(rs).containsOnlyOnce(brian, keith);
    }

    @Test
    public void qualifiedField() {
        dbRule.getJdbi().useHandle(handle -> {
            handle.execute("insert into something(id, name) values (1, 'abc')");

            QualifiedFieldDao dao = handle.attach(QualifiedFieldDao.class);

            assertThat(dao.get(1))
                .isEqualTo(new QualifiedFieldThing(1, "cba"));

            dao.insert(new QualifiedFieldThing(2, "xyz"));

            assertThat(handle.select("SELECT name FROM something WHERE id = 2")
                .mapTo(String.class)
                .findOnly())
                .isEqualTo("zyx");
        });
    }

    @RegisterArgumentFactory(ReversedStringArgumentFactory.class)
    @RegisterColumnMapper(ReversedStringMapper.class)
    public interface QualifiedFieldDao {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindJpa QualifiedFieldThing thing);

        @SqlQuery("select * from something where id = :id")
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        QualifiedFieldThing get(int id);
    }

    @Entity
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualifiedFieldThing {
        @Column
        private int id;

        @Reversed
        @Column
        private String name;
    }

    @Test
    public void qualifiedGetter() {
        dbRule.getJdbi().useHandle(handle -> {
            handle.execute("insert into something(id, name) values (1, 'abc')");

            QualifiedGetterDao dao = handle.attach(QualifiedGetterDao.class);

            assertThat(dao.get(1))
                .isEqualTo(new QualifiedGetterThing(1, "cba"));

            dao.insert(new QualifiedGetterThing(2, "xyz"));

            assertThat(handle.select("SELECT name FROM something WHERE id = 2")
                .mapTo(String.class)
                .findOnly())
                .isEqualTo("zyx");
        });
    }

    @RegisterArgumentFactory(ReversedStringArgumentFactory.class)
    @RegisterColumnMapper(ReversedStringMapper.class)
    public interface QualifiedGetterDao {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindJpa QualifiedGetterThing thing);

        @SqlQuery("select * from something where id = :id")
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        QualifiedGetterThing get(int id);
    }

    @Entity
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualifiedGetterThing {
        private int id;
        private String name;

        @Column
        public int getId() {
            return id;
        }

        @Reversed
        @Column
        public String getName() {
            return name;
        }
    }

    @Test
    public void qualifiedSetter() {
        dbRule.getJdbi().useHandle(handle -> {
            handle.execute("insert into something(id, name) values (1, 'abc')");

            QualifiedSetterDao dao = handle.attach(QualifiedSetterDao.class);

            assertThat(dao.get(1))
                .isEqualTo(new QualifiedSetterThing(1, "cba"));

            dao.insert(new QualifiedSetterThing(2, "xyz"));

            assertThat(handle.select("SELECT name FROM something WHERE id = 2")
                .mapTo(String.class)
                .findOnly())
                .isEqualTo("zyx");
        });
    }

    @RegisterArgumentFactory(ReversedStringArgumentFactory.class)
    @RegisterColumnMapper(ReversedStringMapper.class)
    public interface QualifiedSetterDao {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindJpa QualifiedSetterThing thing);

        @SqlQuery("select * from something where id = :id")
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        QualifiedSetterThing get(int id);
    }

    @Entity
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualifiedSetterThing {
        private int id;
        private String name;

        @Column
        public void setId(int id) {
            this.id = id;
        }

        @Reversed
        @Column
        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void qualifiedSetterParam() {
        dbRule.getJdbi().useHandle(handle -> {
            handle.execute("insert into something(id, name) values (1, 'abc')");

            QualifiedSetterParamDao dao = handle.attach(QualifiedSetterParamDao.class);

            assertThat(dao.get(1))
                .isEqualTo(new QualifiedSetterParamThing(1, "cba"));

            dao.insert(new QualifiedSetterParamThing(2, "xyz"));

            assertThat(handle.select("SELECT name FROM something WHERE id = 2")
                .mapTo(String.class)
                .findOnly())
                .isEqualTo("zyx");
        });
    }

    @RegisterArgumentFactory(ReversedStringArgumentFactory.class)
    @RegisterColumnMapper(ReversedStringMapper.class)
    public interface QualifiedSetterParamDao {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindJpa QualifiedSetterParamThing thing);

        @SqlQuery("select * from something where id = :id")
        @RegisterRowMapperFactory(JpaMapperFactory.class)
        QualifiedSetterParamThing get(int id);
    }

    @Entity
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualifiedSetterParamThing {
        private int id;
        private String name;

        @Column
        public void setId(int id) {
            this.id = id;
        }

        @Column
        public void setName(@Reversed String name) {
            this.name = name;
        }
    }

    private static <T extends Thing> AbstractListAssert<?, ? extends List<? extends T>, T, ?> assertThatThing(List<T> rs) {
        return assertThat(rs).usingElementComparator(Comparator.comparing(Thing::getId).thenComparing(thing -> Objects.toString(thing.getName(), "")));
    }
}
