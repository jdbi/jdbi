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

import java.util.List;

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.sqlobject.SqlObjectBuilder;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.SqlUpdate;
import org.jdbi.v3.sqlobject.customizers.RegisterMapperFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import static org.junit.Assert.assertEquals;

public class JpaTest {
    private static final String INSERT_BY_PROPERTY_NAME = "insert into something(id, name) values (:id, :name)";
    private static final String SELECT_BY_PROPERTY_NAME = "select id, name from something";

    public static final String INSERT_BY_ANNOTATION_NAME = "insert into something (id, name) values (:foo, :bar)";
    public static final String SELECT_BY_ANNOTATION_NAME = "select id as foo, name as bar from something";
    public static final String ID_ANNOTATION_NAME = "foo";
    public static final String NAME_ANNOTATION_NAME = "bar";

    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();
    
    interface Thing {
        int getId();
        String getName();
    }

    @Entity
    static class EntityThing implements Thing {
        private int id;
        private String name;
        
        public EntityThing() {}
        public EntityThing(int id, String name) { setId(id); setName(name); }

        public int getId() { return id; }
        public String getName() { return name; }

        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
    }

    interface EntityThingDao {
        @SqlUpdate(INSERT_BY_PROPERTY_NAME)
        void insert(@BindJpa EntityThing thing);

        @SqlQuery(SELECT_BY_PROPERTY_NAME)
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<EntityThing> list();
    }

    @Test
    public void testEntityNoColumnAnnotations() throws Exception {
        EntityThing brian = new EntityThing(1, "Brian");
        EntityThing keith = new EntityThing(2, "Keith");

        EntityThingDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), EntityThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<EntityThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), brian);
        assertThingEquals(rs.get(1), keith);
    }

    @Entity
    static class FieldThing implements Thing {
        @Column private int id;
        @Column private String name;
        
        public FieldThing() {}
        public FieldThing(int id, String name) { setId(id); setName(name); }

        public int getId() { return id; }
        public String getName() { return name; }

        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
    }

    interface FieldThingDao {
        @SqlUpdate(INSERT_BY_PROPERTY_NAME)
        void insert(@BindJpa FieldThing thing);

        @SqlQuery(SELECT_BY_PROPERTY_NAME)
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<FieldThing> list();
    }

    @Test
    public void testField() throws Exception {
        FieldThing brian = new FieldThing(1, "Brian");
        FieldThing keith = new FieldThing(2, "Keith");

        FieldThingDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), FieldThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<FieldThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), brian);
        assertThingEquals(rs.get(1), keith);
    }

    @Entity
    static class NamedFieldThing implements Thing {
        @Column(name = "foo") private int id;
        @Column(name = "bar") private String name;

        public NamedFieldThing() {}
        public NamedFieldThing(int id, String name) { setId(id); setName(name); }

        public int getId() { return id; }
        public String getName() { return name; }

        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
    }

    interface NamedFieldThingDao {
        @SqlUpdate(INSERT_BY_ANNOTATION_NAME)
        void insert(@BindJpa NamedFieldThing thing);

        @SqlQuery(SELECT_BY_ANNOTATION_NAME)
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<NamedFieldThing> list();
    }

    @Test
    public void testNamedField() throws Exception {
        NamedFieldThing brian = new NamedFieldThing(1, "Brian");
        NamedFieldThing keith = new NamedFieldThing(2, "Keith");

        NamedFieldThingDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), NamedFieldThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<NamedFieldThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), brian);
        assertThingEquals(rs.get(1), keith);
    }

    @Entity
    static class GetterThing implements Thing {
        private int id;
        private String name;

        public GetterThing() {}
        public GetterThing(int id, String name) { setId(id); setName(name); }

        @Column public int getId() { return id; }
        @Column public String getName() { return name; }

        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
    }

    interface GetterThingDao {
        @SqlUpdate(INSERT_BY_PROPERTY_NAME)
        void insert(@BindJpa GetterThing thing);

        @SqlQuery(SELECT_BY_PROPERTY_NAME)
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<GetterThing> list();
    }

    @Test
    public void testGetter() throws Exception {
        GetterThing brian = new GetterThing(1, "Brian");
        GetterThing keith = new GetterThing(2, "Keith");

        GetterThingDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), GetterThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<GetterThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), brian);
        assertThingEquals(rs.get(1), keith);
    }

    @Entity
    static class NamedGetterThing implements Thing {
        private int id;
        private String name;

        public NamedGetterThing() {}
        public NamedGetterThing(int id, String name) { setId(id); setName(name); }

        @Column(name = "foo") public int getId() { return id; }
        @Column(name = "bar") public String getName() { return name; }

        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
    }

    interface NamedGetterThingDao {
        @SqlUpdate(INSERT_BY_ANNOTATION_NAME)
        void insert(@BindJpa NamedGetterThing thing);

        @SqlQuery(SELECT_BY_ANNOTATION_NAME)
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<NamedGetterThing> list();
    }

    @Test
    public void testNamedGetter() throws Exception {
        NamedGetterThing brian = new NamedGetterThing(1, "Brian");
        NamedGetterThing keith = new NamedGetterThing(2, "Keith");

        NamedGetterThingDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), NamedGetterThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<NamedGetterThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), brian);
        assertThingEquals(rs.get(1), keith);
    }

    @Entity
    static class SetterThing implements Thing {
        private int id;
        private String name;

        public SetterThing() {}
        public SetterThing(int id, String name) { setId(id); setName(name); }

        public int getId() { return id; }
        public String getName() { return name; }

        @Column public void setId(int id) { this.id = id; }
        @Column public void setName(String name) { this.name = name; }
    }

    interface SetterThingDao {
        @SqlUpdate(INSERT_BY_PROPERTY_NAME)
        void insert(@BindJpa SetterThing thing);

        @SqlQuery(SELECT_BY_PROPERTY_NAME)
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<SetterThing> list();
    }

    @Test
    public void testSetter() throws Exception {
        SetterThing brian = new SetterThing(1, "Brian");
        SetterThing keith = new SetterThing(2, "Keith");

        SetterThingDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), SetterThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<SetterThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), brian);
        assertThingEquals(rs.get(1), keith);
    }

    @Entity
    static class NamedSetterThing implements Thing {
        private int id;
        private String name;

        public NamedSetterThing() {}
        public NamedSetterThing(int id, String name) { setId(id); setName(name); }

        public int getId() { return id; }
        public String getName() { return name; }

        @Column(name = "foo") public void setId(int id) { this.id = id; }
        @Column(name = "bar") public void setName(String name) { this.name = name; }
    }

    interface NamedSetterThingDao {
        @SqlUpdate(INSERT_BY_ANNOTATION_NAME)
        void insert(@BindJpa NamedSetterThing thing);

        @SqlQuery(SELECT_BY_ANNOTATION_NAME)
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<NamedSetterThing> list();
    }

    @Test
    public void testNamedSetter() throws Exception {
        NamedSetterThing brian = new NamedSetterThing(1, "Brian");
        NamedSetterThing keith = new NamedSetterThing(2, "Keith");

        NamedSetterThingDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), NamedSetterThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<NamedSetterThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), brian);
        assertThingEquals(rs.get(1), keith);
    }

    @MappedSuperclass
    static class MappedSuperclassThing {
        private int id;
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
    }

    @Entity
    static class ExtendsMappedSuperclassThing extends MappedSuperclassThing implements Thing {
        public ExtendsMappedSuperclassThing() {}
        public ExtendsMappedSuperclassThing(int id, String name) { setId(id); setName(name); }

        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    interface MappedSuperclassThingDao {
        @SqlUpdate(INSERT_BY_PROPERTY_NAME)
        void insert(@BindJpa ExtendsMappedSuperclassThing thing);

        @SqlQuery(SELECT_BY_PROPERTY_NAME)
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<ExtendsMappedSuperclassThing> list();
    }

    @Test
    public void testMappedSuperclass() throws Exception {
        ExtendsMappedSuperclassThing brian = new ExtendsMappedSuperclassThing(1, "Brian");
        ExtendsMappedSuperclassThing keith = new ExtendsMappedSuperclassThing(2, "Keith");

        MappedSuperclassThingDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), MappedSuperclassThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<ExtendsMappedSuperclassThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), brian);
        assertThingEquals(rs.get(1), keith);
    }

    @Entity
    static class AnnotationPriorityThing implements Thing {
        @Column(name = ID_ANNOTATION_NAME) private int id;
        private String name;

        public AnnotationPriorityThing() {}
        public AnnotationPriorityThing(int id, String name) { setId(id); setName(name); }

        @Column(name = "ignored") public int getId() { return id; }
        @Column(name = NAME_ANNOTATION_NAME) public String getName() { return name; }

        @Column(name = "ignored") public void setId(int id) { this.id = id; }
        @Column(name = "ignored") public void setName(String name) { this.name = name; }
    }

    interface AnnotationPriorityThingDao {
        @SqlUpdate(INSERT_BY_ANNOTATION_NAME)
        void insert(@BindJpa AnnotationPriorityThing thing);

        @SqlQuery(SELECT_BY_ANNOTATION_NAME)
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<AnnotationPriorityThing> list();
    }

    @Test
    public void testAnnotationPriority() throws Exception {
        // fields before getters before setters
        AnnotationPriorityThing brian = new AnnotationPriorityThing(1, "Brian");
        AnnotationPriorityThing keith = new AnnotationPriorityThing(2, "Keith");

        AnnotationPriorityThingDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), AnnotationPriorityThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<AnnotationPriorityThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), brian);
        assertThingEquals(rs.get(1), keith);
    }

    @Entity
    static class TransientFieldThing implements Thing {
        private int id;
        @Transient private String name;

        public TransientFieldThing() {}
        public TransientFieldThing(int id, String name) { setId(id); setName(name); }

        public int getId() { return id; }
        public String getName() { return name; }

        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
    }

    interface TransientFieldThingDao {
        @SqlUpdate("insert into something(id, name) values (:id, 'dummy')")
        void insert(@BindJpa TransientFieldThing thing);

        @SqlQuery(SELECT_BY_PROPERTY_NAME)
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<TransientFieldThing> list();
    }

    @Test
    public void testTransientField() throws Exception {
        // fields before getters before setters
        TransientFieldThing brian = new TransientFieldThing(1, "Brian");
        TransientFieldThing keith = new TransientFieldThing(2, "Keith");

        TransientFieldThingDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), TransientFieldThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<TransientFieldThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), new TransientFieldThing(1, null));
        assertThingEquals(rs.get(1), new TransientFieldThing(2, null));
    }

    @Entity
    static class TransientGetterThing implements Thing {
        private int id;
        private String name;

        public TransientGetterThing() {}
        public TransientGetterThing(int id, String name) { setId(id); setName(name); }

        public int getId() { return id; }
        @Transient public String getName() { return name; }

        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
    }

    interface TransientGetterThingDao {
        @SqlUpdate("insert into something(id, name) values (:id, 'dummy')")
        void insert(@BindJpa TransientGetterThing thing);

        @SqlQuery(SELECT_BY_PROPERTY_NAME)
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<TransientGetterThing> list();
    }

    @Test
    public void testTransientGetter() throws Exception {
        // fields before getters before setters
        TransientGetterThing brian = new TransientGetterThing(1, "Brian");
        TransientGetterThing keith = new TransientGetterThing(2, "Keith");

        TransientGetterThingDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), TransientGetterThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<TransientGetterThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), new TransientGetterThing(1, null));
        assertThingEquals(rs.get(1), new TransientGetterThing(2, null));
    }

    @Entity
    static class TransientSetterThing implements Thing {
        private int id;
        private String name;

        public TransientSetterThing() {}
        public TransientSetterThing(int id, String name) { setId(id); setName(name); }

        public int getId() { return id; }
        public String getName() { return name; }

        public void setId(int id) { this.id = id; }
        @Transient public void setName(String name) { this.name = name; }
    }

    interface TransientSetterThingDao {
        @SqlUpdate("insert into something(id, name) values (:id, 'dummy')")
        void insert(@BindJpa TransientSetterThing thing);

        @SqlQuery(SELECT_BY_PROPERTY_NAME)
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<TransientSetterThing> list();
    }

    @Test
    public void testTransientSetter() throws Exception {
        // fields before getters before setters
        TransientSetterThing brian = new TransientSetterThing(1, "Brian");
        TransientSetterThing keith = new TransientSetterThing(2, "Keith");

        TransientSetterThingDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), TransientSetterThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<TransientSetterThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), new TransientSetterThing(1, null));
        assertThingEquals(rs.get(1), new TransientSetterThing(2, null));
    }

    interface SuperfluousColumnDao {
        @SqlUpdate(INSERT_BY_PROPERTY_NAME)
        void insert(@BindJpa FieldThing thing);

        @SqlQuery("select id, name, 'Rob Schneider' as extra from something")
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<FieldThing> list();
    }
    
    @Test
    public void testMapWithSuperfluousColumn() {
        FieldThing brian = new FieldThing(1, "Brian");
        FieldThing keith = new FieldThing(2, "Keith");

        SuperfluousColumnDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), SuperfluousColumnDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<FieldThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), brian);
        assertThingEquals(rs.get(1), keith);
    }

    interface MissingColumnDao {
        @SqlUpdate("insert into something(id) values (:id)")
        void insert(@BindJpa FieldThing thing);

        @SqlQuery("select id from something")
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<FieldThing> list();
    }

    @Test
    public void testMissingColumn() {
        FieldThing brian = new FieldThing(1, "Brian");
        FieldThing keith = new FieldThing(2, "Keith");

        MissingColumnDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), MissingColumnDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<FieldThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), new FieldThing(1, null));
        assertThingEquals(rs.get(1), new FieldThing(2, null));
    }

    @MappedSuperclass
    static class OverriddenSuperclassThing implements Thing {
        @Column(name = "foo") private int id;
        @Column(name = "bar") private String name;

        public int getId() { return id; }
        public String getName() { return name; }

        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
    }

    @Entity
    static class OverridingSubclassThing extends OverriddenSuperclassThing {
        public OverridingSubclassThing() {}
        public OverridingSubclassThing(int id, String name) { setId(id); setName(name); }

        @Override @Column(name = "meow") public int getId() { return super.getId(); }
    }

    interface OverridingSubclassThingDao {
        @SqlUpdate("insert into something(id, name) values (:meow, :bar)")
        void insert(@BindJpa OverridingSubclassThing thing);

        @SqlQuery("select id as meow, name as bar from something")
        @RegisterMapperFactory(JpaMapperFactory.class)
        List<OverridingSubclassThing> list();
    }

    @Test
    public void subclassAnnotationOverridesSuperclass() {
        // Annotated takes precedence over no annotation, even if annotated in superclass
        // Annotated member in subclass takes precedence over annotated member in superclass

        OverridingSubclassThing brian = new OverridingSubclassThing(1, "Brian");
        OverridingSubclassThing keith = new OverridingSubclassThing(2, "Keith");

        OverridingSubclassThingDao dao = SqlObjectBuilder.attach(db.getSharedHandle(), OverridingSubclassThingDao.class);
        dao.insert(brian);
        dao.insert(keith);

        List<OverridingSubclassThing> rs = dao.list();

        assertEquals(rs.size(), 2);
        assertThingEquals(rs.get(0), brian);
        assertThingEquals(rs.get(1), keith);
    }

    public static void assertThingEquals(Thing one, Thing two) {
        assertEquals(one.getId(), two.getId());
        assertEquals(one.getName(), two.getName());
    }
}
