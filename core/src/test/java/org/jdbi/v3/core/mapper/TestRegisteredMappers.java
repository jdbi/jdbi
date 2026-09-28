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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Reversed;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TestRegisteredMappers {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    @Test
    public void testRegisterInferredOnJdbi() {
        Jdbi db = h2Extension.getJdbi();

        db.registerRowMapper(new SomethingMapper());
        Something sam = db.withHandle(handle1 -> {
            handle1.execute("insert into something (id, name) values (18, 'Sam')");

            return handle1.createQuery("select id, name from something where id = :id")
                .bind("id", 18)
                .mapTo(Something.class)
                .one();
        });

        assertThat(sam.getName()).isEqualTo("Sam");
    }

    @Test
    public void registerByGenericType() {
        Jdbi db = h2Extension.getJdbi();

        @SuppressWarnings("unchecked")
        RowMapper<Iterable<Calendar>> mapper = mock(RowMapper.class);
        GenericType<Iterable<Calendar>> iterableOfCalendarType = new GenericType<Iterable<Calendar>>() {};

        db.registerRowMapper(iterableOfCalendarType, mapper);

        assertThat(db.getConfig(RowMappers.class).findFor(iterableOfCalendarType))
            .contains(mapper);
    }

    @Test
    public void registerColumnMapperWithRedundantWildcard() {
        Jdbi db = h2Extension.getJdbi();

        db.registerColumnMapper(new GenericType<List<? extends String>>() {}, (rs, col, ctx) -> Arrays.asList(rs.getString(col).split(",")));

        List<String> result = db.withHandle(h -> h.createQuery("select 'a,b'").mapTo(new GenericType<List<String>>() {}).one());

        assertThat(result).containsExactly("a", "b");
    }

    @Test
    public void registerColumnMapperMatchesRedundantWildcard() {
        Jdbi db = h2Extension.getJdbi();

        db.registerColumnMapper(new GenericType<List<String>>() {}, (rs, col, ctx) -> Arrays.asList(rs.getString(col).split(",")));

        List<? extends String> result = db.withHandle(h -> h.createQuery("select 'a,b'").mapTo(new GenericType<List<? extends String>>() {}).one());

        assertThat(result).isEqualTo(List.of("a", "b"));
    }

    @Test
    public void registerQualifiedColumnMapperWithRedundantWildcard() {
        Jdbi db = h2Extension.getJdbi();

        db.registerColumnMapper(QualifiedType.of(new GenericType<List<? extends String>>() {}).with(Reversed.class),
            (rs, col, ctx) -> Arrays.asList(rs.getString(col).split(",")));

        List<String> result = db.withHandle(h -> h.createQuery("select 'a,b'")
                .mapTo(QualifiedType.of(new GenericType<List<String>>() {}).with(Reversed.class))
                .one());

        assertThat(result).containsExactly("a", "b");
    }

    @Test
    public void registerRowMapperWithRedundantWildcard() {
        Jdbi db = h2Extension.getJdbi();

        db.registerRowMapper(new GenericType<List<? extends String>>() {}, (rs, ctx) -> Arrays.asList(rs.getString(1).split(",")));

        List<String> result = db.withHandle(h -> h.createQuery("select 'a,b'").mapTo(new GenericType<List<String>>() {}).one());

        assertThat(result).containsExactly("a", "b");
    }

    @Test
    public void registerColumnMapperKeepsOpenWildcard() {
        Jdbi db = h2Extension.getJdbi();

        ColumnMapper<List<? extends CharSequence>> mapper = (rs, col, ctx) -> Arrays.asList(rs.getString(col).split(","));
        db.registerColumnMapper(new GenericType<List<? extends CharSequence>>() {}, mapper);

        assertThat(db.getConfig(ColumnMappers.class).findFor(new GenericType<List<? extends CharSequence>>() {})).contains(mapper);
        assertThat(db.getConfig(ColumnMappers.class).findFor(new GenericType<List<CharSequence>>() {})).isNotEqualTo(Optional.of(mapper));
    }

    static class WildcardListColumnMapper implements ColumnMapper<List<? extends String>> {
        @Override
        public List<? extends String> map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
            return Arrays.asList(r.getString(columnNumber).split(","));
        }
    }

    static class WildcardListRowMapper implements RowMapper<List<? extends String>> {
        @Override
        public List<? extends String> map(ResultSet rs, StatementContext ctx) throws SQLException {
            return Arrays.asList(rs.getString(1).split(","));
        }
    }

    @Test
    public void registerInferredColumnMapperWithRedundantWildcard() {
        Jdbi db = h2Extension.getJdbi();

        db.registerColumnMapper(new WildcardListColumnMapper());

        List<String> result = db.withHandle(h -> h.createQuery("select 'a,b'").mapTo(new GenericType<List<String>>() {}).one());

        assertThat(result).containsExactly("a", "b");
    }

    @Test
    public void registerInferredRowMapperWithRedundantWildcard() {
        Jdbi db = h2Extension.getJdbi();

        db.registerRowMapper(new WildcardListRowMapper());

        List<String> result = db.withHandle(h -> h.createQuery("select 'a,b'").mapTo(new GenericType<List<String>>() {}).one());

        assertThat(result).containsExactly("a", "b");
    }

    @Test
    public void laterRegistrationWinsAcrossRedundantWildcard() {
        Jdbi db = h2Extension.getJdbi();

        ColumnMapper<List<String>> first = (rs, col, ctx) -> List.of("first");
        ColumnMapper<List<? extends String>> second = (rs, col, ctx) -> List.of("second");
        db.registerColumnMapper(new GenericType<List<String>>() {}, first);
        db.registerColumnMapper(new GenericType<List<? extends String>>() {}, second);

        assertThat(db.getConfig(ColumnMappers.class).findFor(new GenericType<List<String>>() {}).orElseThrow()).isSameAs(second);
    }
}
