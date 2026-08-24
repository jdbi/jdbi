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
package org.jdbi.v3.core.result;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestRowViewPrefixedMappers {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private Handle h;

    @BeforeEach
    public void setUp() {
        h = h2Extension.getSharedHandle();
        h.execute("create table contacts (a_id int, a_name varchar(50), b_id int, b_name varchar(50))");
        h.execute("insert into contacts values (1, 'alice', 2, 'bob')");
    }

    @Test
    public void testBeanMapperByPrefix() {
        h.registerRowMapper(BeanMapper.factory(ContactBean.class, "a"));
        h.registerRowMapper(BeanMapper.factory(ContactBean.class, "b"));

        Map<String, ContactBean> result = reduceByPrefix();

        assertThat(result.get("a").getName()).isEqualTo("alice");
        assertThat(result.get("a").getId()).isEqualTo(1);
        assertThat(result.get("b").getName()).isEqualTo("bob");
        assertThat(result.get("b").getId()).isEqualTo(2);
    }

    @Test
    public void testConstructorMapperByPrefix() {
        h.registerRowMapper(ConstructorMapper.factory(ContactCtor.class, "a"));
        h.registerRowMapper(ConstructorMapper.factory(ContactCtor.class, "b"));

        Map<String, ContactCtor> result = h.createQuery("select * from contacts")
            .reduceRows(new HashMap<>(), (map, rv) -> {
                map.put("a", rv.getRow(ContactCtor.class, "a"));
                map.put("b", rv.getRow(ContactCtor.class, "b"));
                return map;
            });

        assertThat(result.get("a").name).isEqualTo("alice");
        assertThat(result.get("b").name).isEqualTo("bob");
    }

    @Test
    public void testFieldMapperByPrefix() {
        h.registerRowMapper(FieldMapper.factory(ContactField.class, "a"));
        h.registerRowMapper(FieldMapper.factory(ContactField.class, "b"));

        Map<String, ContactField> result = h.createQuery("select * from contacts")
            .reduceRows(new HashMap<>(), (map, rv) -> {
                map.put("a", rv.getRow(ContactField.class, "a"));
                map.put("b", rv.getRow(ContactField.class, "b"));
                return map;
            });

        assertThat(result.get("a").name).isEqualTo("alice");
        assertThat(result.get("b").name).isEqualTo("bob");
    }

    @Test
    public void testGenericTypeOverload() {
        h.registerRowMapper(BeanMapper.factory(ContactBean.class, "b"));

        ContactBean bob = h.createQuery("select * from contacts")
            .reduceRows((ContactBean) null, (acc, rv) -> rv.getRow(new GenericType<ContactBean>() {}, "b"));

        assertThat(bob.getName()).isEqualTo("bob");
    }

    @Test
    public void testUnprefixedLookupKeepsLastRegisteredWinsBehavior() {
        h.registerRowMapper(BeanMapper.factory(ContactBean.class, "a"));
        h.registerRowMapper(BeanMapper.factory(ContactBean.class, "b"));

        ContactBean bean = h.createQuery("select * from contacts")
            .reduceRows((ContactBean) null, (acc, rv) -> rv.getRow(ContactBean.class));

        assertThat(bean.getName()).isEqualTo("bob");
    }

    @Test
    public void testEmptyPrefixMatchesMapperRegisteredWithoutPrefix() {
        h.registerRowMapper(BeanMapper.factory(ContactBean.class));

        ContactBean alice = h.createQuery("select a_id as id, a_name as name from contacts")
            .reduceRows((ContactBean) null, (acc, rv) -> rv.getRow(ContactBean.class, ""));

        assertThat(alice.getName()).isEqualTo("alice");
    }

    @Test
    public void testUnknownPrefixThrows() {
        h.registerRowMapper(BeanMapper.factory(ContactBean.class, "a"));

        assertThatThrownBy(() -> h.createQuery("select * from contacts")
            .reduceRows((ContactBean) null, (acc, rv) -> rv.getRow(ContactBean.class, "c")))
            .isInstanceOf(NoSuchMapperException.class)
            .hasMessageContaining("with prefix \"c\"");
    }

    @Test
    public void testPlainRowMapperNeverMatchesAPrefix() {
        h.registerRowMapper(ContactBean.class, (rs, ctx) -> {
            ContactBean bean = new ContactBean();
            bean.setId(rs.getInt("a_id"));
            bean.setName(rs.getString("a_name"));
            return bean;
        });

        assertThatThrownBy(() -> h.createQuery("select * from contacts")
            .reduceRows((ContactBean) null, (acc, rv) -> rv.getRow(ContactBean.class, "a")))
            .isInstanceOf(NoSuchMapperException.class);
    }

    @Test
    public void testNullPrefixRejected() {
        h.registerRowMapper(BeanMapper.factory(ContactBean.class, "a"));

        assertThatThrownBy(() -> h.createQuery("select * from contacts")
            .reduceRows((ContactBean) null, (acc, rv) -> rv.getRow(ContactBean.class, null)))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testRegistryLookupByPrefix() {
        RowMappers rowMappers = h.getConfig(RowMappers.class);

        assertThat(rowMappers.findFor((Type) ContactBean.class, "a")).isEmpty();

        h.registerRowMapper(BeanMapper.factory(ContactBean.class, "a"));

        assertThat(rowMappers.findFor((Type) ContactBean.class, "a")).isPresent();
        assertThat(rowMappers.findFor((Type) ContactBean.class, "b")).isEmpty();
        assertThatThrownBy(() -> rowMappers.findFor((Type) ContactBean.class, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testPrefixComparisonIsExact() {
        RowMappers rowMappers = h.getConfig(RowMappers.class);
        h.registerRowMapper(BeanMapper.factory(ContactBean.class, "a"));

        assertThat(rowMappers.findFor((Type) ContactBean.class, "A")).isEmpty();
        assertThat(rowMappers.findFor((Type) ContactBean.class, "a_")).isEmpty();
    }

    @Test
    public void testRowViewBaseClassRejectsPrefixedLookup() {
        RowView rowView = new RowView() {
            @Override
            public Object getRow(Type type) {
                return null;
            }

            @Override
            public <T> T getColumn(int column, QualifiedType<T> type) {
                return null;
            }

            @Override
            public <T> T getColumn(String column, QualifiedType<T> type) {
                return null;
            }
        };

        assertThatThrownBy(() -> rowView.getRow(ContactBean.class, "a"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private Map<String, ContactBean> reduceByPrefix() {
        return h.createQuery("select * from contacts")
            .reduceRows(new HashMap<>(), (map, rv) -> {
                map.put("a", rv.getRow(ContactBean.class, "a"));
                map.put("b", rv.getRow(ContactBean.class, "b"));
                return map;
            });
    }

    public static class ContactBean {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ContactCtor {
        final int id;
        final String name;

        public ContactCtor(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class ContactField {
        public int id;
        public String name;
    }
}
