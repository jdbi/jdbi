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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.meta.Beta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestRowView {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @BeforeEach
    public void setUp() {
        Handle h = h2Extension.getSharedHandle();
        h.execute("CREATE TABLE test (a INT)");
        for (int a = 0; a < 5; a++) {
            h.execute("INSERT INTO test VALUES (?)", a);
        }
    }

    @Test
    public void testRowViewClass() {
        assertThat(
            h2Extension.getSharedHandle().createQuery("SELECT * FROM test")
                .reduceRows(0, (a, rv) -> a + rv.getColumn("a", Integer.class)))
            .isEqualTo(10);
    }

    @Test
    public void testRowViewUntypedColumn() {
        assertThat(
            h2Extension.getSharedHandle().createQuery("SELECT * FROM test")
                .reduceRows(0, (a, rv) -> a + (Integer) rv.getColumn("a")))
            .isEqualTo(10);
        assertThat(
            h2Extension.getSharedHandle().createQuery("SELECT * FROM test")
                .reduceRows(0, (a, rv) -> a + (Integer) rv.getColumn(1)))
            .isEqualTo(10);
        assertThat(
            h2Extension.getSharedHandle().createQuery("SELECT CAST(NULL AS INT) a")
                .reduceRows(new ArrayList<>(), (list, rv) -> {
                    list.add(rv.getColumn("a"));
                    return list;
                }))
            .containsExactly((Object) null);
    }

    @Test
    public void testRowViewColumnNames() {
        assertThat(
            h2Extension.getSharedHandle().createQuery("SELECT a, a AS other FROM test")
                .reduceRows(new ArrayList<List<String>>(), (list, rv) -> {
                    list.add(rv.getColumnNames());
                    return list;
                }))
            .allSatisfy(names -> assertThat(names).containsExactly("A", "OTHER"));
    }

    @Test
    public void testRowViewGetRowWithMapper() {
        AtomicInteger specializations = new AtomicInteger();
        RowMapper<Integer> mapper = new RowMapper<>() {
            @Override
            public Integer map(ResultSet rs, StatementContext ctx) throws SQLException {
                return rs.getInt("a") * 10;
            }

            @Override
            public RowMapper<Integer> specialize(ResultSet rs, StatementContext ctx) {
                specializations.incrementAndGet();
                return this;
            }
        };

        assertThat(
            h2Extension.getSharedHandle().createQuery("SELECT * FROM test")
                .reduceRows(0, (a, rv) -> a + rv.getRow(mapper)))
            .isEqualTo(100);
        assertThat(specializations).hasValue(1);
    }

    @Test
    public void testRowViewQualifiedType() {
        assertThatThrownBy(() ->
            h2Extension.getSharedHandle().createQuery("SELECT * FROM test")
                .reduceRows(0, (a, rv) -> a + rv.getColumn("a", QualifiedType.of(int.class).with(Beta.class))))
            .isInstanceOf(NoSuchMapperException.class);
    }
}
