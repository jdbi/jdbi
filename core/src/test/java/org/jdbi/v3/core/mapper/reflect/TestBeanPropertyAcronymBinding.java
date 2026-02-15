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
package org.jdbi.v3.core.mapper.reflect;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that bean properties whose names start with a single uppercase letter followed
 * by another uppercase letter (e.g. getPCode()) can be bound using the intuitive
 * lowercased form (:pCode). See GitHub issue #897.
 *
 * Per Introspector.decapitalize(), getPCode() yields property name "PCode", not "pCode",
 * because both the first and second characters are uppercase.
 */
public class TestBeanPropertyAcronymBinding {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private Handle handle;

    @BeforeEach
    public void setUp() {
        this.handle = h2Extension.getSharedHandle();
        handle.execute("CREATE TABLE items (id INT, p_code VARCHAR(50))");
        handle.execute("INSERT INTO items (id, p_code) VALUES (1, 'ABC')");
    }

    @Test
    public void testBindBeanWithAcronymPropertyPrefixed() {
        AcronymBean bean = new AcronymBean();
        bean.setId(2);
        bean.setPCode("DEF");

        handle.createUpdate("INSERT INTO items (id, p_code) VALUES (:bean.id, :bean.pCode)")
                .bindBean("bean", bean)
                .execute();

        String result = handle.createQuery("SELECT p_code FROM items WHERE id = 2")
                .mapTo(String.class)
                .one();

        assertThat(result).isEqualTo("DEF");
    }

    @Test
    public void testBindBeanWithAcronymPropertyDirect() {
        AcronymBean bean = new AcronymBean();
        bean.setId(3);
        bean.setPCode("GHI");

        handle.createUpdate("INSERT INTO items (id, p_code) VALUES (:id, :pCode)")
                .bindBean(bean)
                .execute();

        String result = handle.createQuery("SELECT p_code FROM items WHERE id = 3")
                .mapTo(String.class)
                .one();

        assertThat(result).isEqualTo("GHI");
    }

    @Test
    public void testOriginalPropertyNameStillWorks() {
        // The original Introspector-reported name "PCode" should also still work
        AcronymBean bean = new AcronymBean();
        bean.setId(4);
        bean.setPCode("JKL");

        handle.createUpdate("INSERT INTO items (id, p_code) VALUES (:id, :PCode)")
                .bindBean(bean)
                .execute();

        String result = handle.createQuery("SELECT p_code FROM items WHERE id = 4")
                .mapTo(String.class)
                .one();

        assertThat(result).isEqualTo("JKL");
    }

    @Test
    public void testMapBeanWithAcronymProperty() {
        handle.registerRowMapper(BeanMapper.factory(AcronymBean.class));

        AcronymBean bean = handle.createQuery("SELECT id, p_code AS pCode FROM items WHERE id = 1")
                .mapTo(AcronymBean.class)
                .one();

        assertThat(bean.getId()).isOne();
        assertThat(bean.getPCode()).isEqualTo("ABC");
    }

    public static class AcronymBean {
        private int id;
        private String pCode;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        // Per Introspector.decapitalize(), this yields property name "PCode" (not "pCode")
        // because both first and second characters of the property part are uppercase.
        public String getPCode() {
            return pCode;
        }

        public void setPCode(String pCode) {
            this.pCode = pCode;
        }
    }
}
