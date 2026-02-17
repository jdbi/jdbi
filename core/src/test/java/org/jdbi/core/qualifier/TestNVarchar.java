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
package org.jdbi.core.qualifier;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.jdbi.core.argument.Arguments;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.junit5.H2DatabaseExtension;
import org.jdbi.core.mapper.ColumnMappers;
import org.jdbi.core.mapper.Mappers;
import org.jdbi.core.statement.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestNVarchar {
    private static final QualifiedType<String> NVARCHAR_STRING = QualifiedType.of(String.class).with(NVarchar.class);

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @BeforeEach
    public void setUp() {
        h2Extension.getJdbi().useHandle(handle ->
            handle.execute("create table nvarchars (id int primary key, name nvarchar not null)"));
    }

    @AfterEach
    public void tearDown() {
        h2Extension.getJdbi().useHandle(handle ->
            handle.execute("drop table nvarchars"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void sqlStatementBindNVarchar() {
        h2Extension.getJdbi().useHandle(handle -> {
            handle.createUpdate("INSERT INTO nvarchars (id, name) VALUES (?, ?)")
                .bind(0, 1)
                .bindNVarchar(1, "foo")
                .execute();

            handle.createUpdate("INSERT INTO nvarchars (id, name) VALUES (:id, :name)")
                .bind("id", 2)
                .bindNVarchar("name", "bar")
                .execute();

            handle.createUpdate("INSERT INTO nvarchars (id, name) VALUES (?, ?)")
                .bind(0, 3)
                .bindByType(1, "baz", NVARCHAR_STRING)
                .execute();

            handle.createUpdate("INSERT INTO nvarchars (id, name) VALUES (:id, :name)")
                .bind("id", 4)
                .bindByType("name", "qux", NVARCHAR_STRING)
                .execute();

            assertThat(
                handle.select("SELECT name FROM nvarchars ORDER BY id")
                    .mapTo(QualifiedType.of(String.class).with(NVarchar.class))
                    .list())
                .containsExactly("foo", "bar", "baz", "qux");

            assertThat(
                handle.select("SELECT name FROM nvarchars ORDER BY id")
                    .mapTo(QualifiedType.of(new GenericType<String>() {}).with(NVarchar.class))
                    .list())
                .containsExactly("foo", "bar", "baz", "qux");

            List rawList = handle.select("SELECT name FROM nvarchars ORDER BY id")
                .mapTo((QualifiedType<?>) NVARCHAR_STRING)
                .list();
            assertThat(rawList)
                .containsExactly("foo", "bar", "baz", "qux");
        });
    }

    /*
     * The databases we test with don't care whether you call setString() and setNString(), which
     * makes it difficult to test that the NVarchar qualifier is being honored on a live database.
     * The following tests are best effort isolation tests to confirm that the @NVarchar String
     * qualified type is being bound and mapped using PreparedStatement.setNString() and
     * ResultSet.getNString(), respectively.
     */

    @Test
    public void findNVarcharArgument() throws Exception {
        h2Extension.getJdbi().useHandle(handle -> {
            String value = "foo";

            PreparedStatement stmt = mock(PreparedStatement.class);

            handle.getConfig(Arguments.class)
                .findFor(NVARCHAR_STRING, value)
                .orElseThrow(IllegalStateException::new)
                .apply(1, stmt, null);

            verify(stmt).setNString(1, value);

            try (Query query = handle.createQuery("no execute")) {
                query.getContext()
                    .findArgumentFor(NVARCHAR_STRING, value)
                    .orElseThrow(IllegalStateException::new)
                    .apply(2, stmt, null);
            }

            verify(stmt).setNString(2, value);
        });
    }

    @Test
    public void findNVarcharMapper() throws Exception {
        h2Extension.getJdbi().useHandle(handle -> {
            ResultSet rs = mock(ResultSet.class);
            when(rs.getNString(anyInt())).thenReturn("value");

            assertThat(
                handle.getConfig(Mappers.class)
                    .findFor(NVARCHAR_STRING)
                    .orElseThrow(IllegalStateException::new)
                    .map(rs, null))
                .isEqualTo("value");

            assertThat(
                handle.getConfig(ColumnMappers.class)
                    .findFor(NVARCHAR_STRING)
                    .orElseThrow(IllegalStateException::new)
                    .map(rs, 1, null))
                .isEqualTo("value");
        });
    }
}
