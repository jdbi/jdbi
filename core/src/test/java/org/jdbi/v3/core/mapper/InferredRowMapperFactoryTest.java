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

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InferredRowMapperFactoryTest {

    @Test
    public void acceptConcreteMapper() {
        RowMapperFactory factory = new InferredRowMapperFactory(new DummyMapper());

        Optional<RowMapper<?>> mapper = factory.build(Dummy.class, new ConfigRegistry());

        assertTrue(mapper.isPresent());
        assertEquals(DummyMapper.class, mapper.get().getClass());
    }

    @Test
    public void rejectGenericMapper() {
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
            () -> new InferredRowMapperFactory(new GenericMapper<>(Dummy.class)));
        assertEquals("Must use a concretely typed RowMapper here", e.getMessage());
    }

    @Test
    public void rejectObjectMapper() {
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, () -> new InferredRowMapperFactory(new ObjectMapper()));
        assertEquals("Must use a concretely typed RowMapper here", e.getMessage());
    }

    @Test
    public void testDetectConcreteMapper() {
        Optional<Type> type = InferredRowMapperFactory.detectType(new DummyMapper());
        assertTrue(type.isPresent());
        assertSame(Dummy.class, type.get());
    }

    @Test
    public void testDetectGenericMapper() {
        Optional<Type> type = InferredRowMapperFactory.detectType(new GenericMapper<>(Dummy.class));
        assertFalse(type.isPresent());
    }

    @Test
    public void testDetectObjectMapper() {
        Optional<Type> type = InferredRowMapperFactory.detectType(new ObjectMapper());
        assertFalse(type.isPresent());
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
