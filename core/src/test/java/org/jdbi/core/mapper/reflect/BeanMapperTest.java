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

package org.jdbi.core.mapper.reflect;

import org.jdbi.core.Handle;
import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.core.internal.testing.H2DatabaseExtension.SOMETHING_INITIALIZER;

public class BeanMapperTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(SOMETHING_INITIALIZER);

    private Handle handle;

    @BeforeEach
    public void setUp() {
        this.handle = h2Extension.getSharedHandle();
        handle.execute("insert into something (id, name) values (1, 'foo')");
    }

    @Test
    public void testColumnNameAnnotation() {
        handle.registerRowMapper(BeanMapper.factory(ColumnNameBean.class));

        ColumnNameBean bean = handle.createQuery("select * from something")
            .mapTo(ColumnNameBean.class)
            .one();

        // annotation on setter
        assertThat(bean.getI()).isOne();
        // annotation on getter
        assertThat(bean.getS()).isEqualTo("foo");
    }

    @Test
    public void testColumnNameMismatch() {
        handle.registerRowMapper(BeanMapper.factory(MismatchColumnNameBean.class));

        MismatchColumnNameBean bean = handle.createQuery("select * from something")
            .mapTo(MismatchColumnNameBean.class)
            .one();

        // annotation on setter
        assertThat(bean.getI()).isOne();
    }

    public static class ColumnNameBean {
        private int i;
        private String s;

        @ColumnName("id")
        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public String getS() {
            return s;
        }

        @ColumnName("name")
        public void setS(String s) {
            this.s = s;
        }
    }

    public static class MismatchColumnNameBean {
        private int i;

        @ColumnName("bad_id")
        public int getI() {
            return i;
        }

        @ColumnName("id")
        public void setI(int i) {
            this.i = i;
        }
    }

}
