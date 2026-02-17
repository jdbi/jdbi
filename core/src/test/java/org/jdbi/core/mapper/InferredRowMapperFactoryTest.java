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
package org.jdbi.core.mapper;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.statement.StatementContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InferredRowMapperFactoryTest {

    @Test
    public void acceptConcreteMapper() {
        RowMapperFactory factory = new InferredRowMapperFactory(new DummyMapper());

        Optional<RowMapper<?>> mapper = factory.build(Dummy.class, new ConfigRegistry());

        assertThat(mapper).isPresent().get()
                .extracting(RowMapper::getClass).isEqualTo(DummyMapper.class);
    }

    @Test
    public void rejectGenericMapper() {
        GenericMapper<Dummy> mapper = new GenericMapper<>(Dummy.class);
        assertThatThrownBy(() -> new InferredRowMapperFactory(mapper))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Must use a concretely typed RowMapper here");
    }

    @Test
    public void rejectObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        assertThatThrownBy(() -> new InferredRowMapperFactory(mapper))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Must use a concretely typed RowMapper here");
    }

    @Test
    public void testDetectConcreteMapper() {
        Optional<Type> type = InferredRowMapperFactory.detectType(new DummyMapper());
        assertThat(type).isPresent().get()
                .isSameAs(Dummy.class);
    }

    @Test
    public void testDetectGenericMapper() {
        Optional<Type> type = InferredRowMapperFactory.detectType(new GenericMapper<>(Dummy.class));
        assertThat(type).isNotPresent();
    }

    @Test
    public void testDetectObjectMapper() {
        Optional<Type> type = InferredRowMapperFactory.detectType(new ObjectMapper());
        assertThat(type).isNotPresent();
    }

    public static class Dummy {}

    public static class DummyMapper implements RowMapper<Dummy> {

        @Override
        public Dummy map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Dummy();
        }
    }

    public static class GenericMapper<T> implements RowMapper<T> {

        public GenericMapper(Class<T> clazz) {}

        @Override
        public T map(ResultSet rs, StatementContext ctx) throws SQLException {
            return null;
        }
    }

    public static class ObjectMapper implements RowMapper<Object> {

        @Override
        public Object map(ResultSet rs, StatementContext ctx) throws SQLException {
            return null;
        }
    }
}
