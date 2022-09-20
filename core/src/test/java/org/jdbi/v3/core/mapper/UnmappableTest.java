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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.annotation.Unmappable;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.junit5.H2DatabaseExtension.SOMETHING_INITIALIZER;

public class UnmappableTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(SOMETHING_INITIALIZER);

    protected Handle handle;

    @BeforeEach
    void setUp() {
        this.handle = h2Extension.getSharedHandle();
    }

    @Test
    public void testUnmappableBeanMapper() {
        TestBean testBean = handle.select("select 1 as id, 2 as unmapped").map(BeanMapper.of(TestBean.class)).one();

        assertThat(testBean).isNotNull();
        assertThat(testBean.getId()).isOne();
        assertThat(testBean.getUnmapped()).isZero();
    }

    public static class TestBean {

        private int id = 0;
        private int unmapped = 0;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getUnmapped() {
            return unmapped;
        }

        @Unmappable
        public void setUnmapped(int unmapped) {
            this.unmapped = unmapped;
        }
    }

    @Test
    public void testUnmappableFieldMapper() {
        TestFieldBean testBean = handle.select("select 1 as id, 2 as unmapped").map(FieldMapper.of(TestFieldBean.class)).one();

        assertThat(testBean).isNotNull();
        assertThat(testBean.id).isOne();
        assertThat(testBean.unmapped).isZero();
    }

    public static class TestFieldBean {

        public int id = 0;

        @Unmappable
        public int unmapped = 0;
    }
}
