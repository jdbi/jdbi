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
package org.jdbi.v3.core.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.jdbi.v3.core.ColumnName;
import org.jdbi.v3.core.H2DatabaseRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ConstructorMapperTest {

    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Before
    public void setUp() throws Exception {
        db.getSharedHandle().registerRowMapper(ConstructorMapper.of(ConstructorBean.class));
        db.getSharedHandle().registerRowMapper(ConstructorMapper.of(NamedParameterBean.class));
        db.getSharedHandle().execute("CREATE TABLE bean (s varchar, i integer)");

        db.getSharedHandle().execute("INSERT INTO bean VALUES('3', 2)");
    }


    @Test
    public void testSimple() throws Exception {
        ConstructorBean bean = db.getSharedHandle().createQuery("SELECT s, i FROM bean").mapTo(ConstructorBean.class).findOnly();

        assertThat(bean.s).isEqualTo("3");
        assertThat(bean.i).isEqualTo(2);
    }

    @Test
    public void testReversed() throws Exception {
        ConstructorBean bean = db.getSharedHandle().createQuery("SELECT i, s FROM bean").mapTo(ConstructorBean.class).findOnly();

        assertThat(bean.s).isEqualTo("3");
        assertThat(bean.i).isEqualTo(2);
    }

    @Test
    public void testExtra() throws Exception {
        ConstructorBean bean = db.getSharedHandle().createQuery("SELECT 1 as ignored, i, s FROM bean").mapTo(ConstructorBean.class).findOnly();

        assertThat(bean.s).isEqualTo("3");
        assertThat(bean.i).isEqualTo(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicate() throws Exception {
        db.getSharedHandle().createQuery("SELECT i, s, s FROM bean").mapTo(ConstructorBean.class).findOnly();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMismatch() throws Exception {
        db.getSharedHandle().createQuery("SELECT i, '7' FROM bean").mapTo(ConstructorBean.class).findOnly();
    }

    @Test
    public void testName() throws Exception {
        NamedParameterBean nb = db.getSharedHandle().createQuery("SELECT 3 AS xyz")
                .mapTo(NamedParameterBean.class).findOnly();
        assertThat(nb.i).isEqualTo(3);
    }

    static class ConstructorBean {
        private final String s;
        private final int i;

        ConstructorBean(String s, int i) {
            this.s = s;
            this.i = i;
        }
    }

    static class NamedParameterBean {
        final int i;
        NamedParameterBean(@ColumnName("xyz") int i) {
            this.i = i;
        }
    }
}
