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
package org.jdbi.e2e.lombok;

import java.util.List;

import lombok.Data;
import org.jdbi.core.Handle;
import org.jdbi.core.mapper.reflect.BeanMapper;
import org.jdbi.core.mapper.reflect.ColumnName;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.JdbiH2Extension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class LombokMappingTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiH2Extension.h2().withInitializer((ds, h) -> {
        h.execute("create table test_table (table_name varchar, is_view boolean)");
        h.execute("insert into test_table (table_name, is_view) values ('abc', 1)");
        h.execute("insert into test_table (table_name, is_view) values ('def', 0)");
        h.registerRowMapper(BooleanObjectTable.class, BeanMapper.of(BooleanObjectTable.class));
        h.registerRowMapper(BooleanPrimitiveTable.class, BeanMapper.of(BooleanPrimitiveTable.class));
    });

    private Handle handle;

    @BeforeEach
    void setUp() {
        handle = h2Extension.getSharedHandle();
    }

    @Test
    public void testIssue2083BooleanObject() {
        BooleanObjectTable t1 = new BooleanObjectTable();
        t1.setTableName("abc");
        t1.setIsView(true);

        BooleanObjectTable t2 = new BooleanObjectTable();
        t2.setTableName("def");
        t2.setIsView(false);

        List<BooleanObjectTable> data = handle.createQuery("SELECT * FROM test_table")
            .mapTo(BooleanObjectTable.class)
            .list();

        assertThat(data).containsExactly(t1, t2);
    }

    @Test
    public void testIssue2083BooleanPrimitive() {
        BooleanPrimitiveTable t1 = new BooleanPrimitiveTable();
        t1.setTableName("abc");
        t1.setView(true);

        BooleanPrimitiveTable t2 = new BooleanPrimitiveTable();
        t2.setTableName("def");
        t2.setView(false);

        List<BooleanPrimitiveTable> data = handle.createQuery("SELECT * FROM test_table")
            .mapTo(BooleanPrimitiveTable.class)
            .list();

        assertThat(data).containsExactly(t1, t2);
    }

    @Data
    public static class BooleanObjectTable {

        private String tableName;
        private Boolean isView;
    }

    @Data
    public static class BooleanPrimitiveTable {

        private String tableName;
        @ColumnName("is_view")
        private boolean isView;
    }
}
