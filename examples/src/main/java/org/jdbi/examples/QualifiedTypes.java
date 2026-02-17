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
package org.jdbi.examples;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.google.common.base.Splitter;
import org.jdbi.core.Jdbi;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.qualifier.Qualifier;
import org.jdbi.core.statement.StatementContext;

/**
 * Demonstrates the use of qualified types. This class contains two column mappers for the same data type (string in SQL, List&lt;String&gt; in Java) that can
 * be differentiated by registering as {@link org.jdbi.core.qualifier.QualifiedType} with different annotations. As the {@link ColumnMapper} classes return a
 * generic type, they use a qualified {@link org.jdbi.core.generic.GenericType}.
 * <p>
 * The use of this code is demonstrated in the test class.
 */
public final class QualifiedTypes {

    private QualifiedTypes() {
        throw new AssertionError("do not instantiate");
    }

    public static void registerMappers(Jdbi jdbi) {
        jdbi.registerColumnMapper(QualifiedType.of(new GenericType<List<String>>() {}).with(Colon.class), new ColonMapper());
        jdbi.registerColumnMapper(QualifiedType.of(new GenericType<List<String>>() {}).with(Comma.class), new CommaMapper());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
    @Qualifier
    public @interface Comma {}

    public static class CommaMapper implements ColumnMapper<List<String>> {
        @Override
        public List<String> map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
            String value = rs.getString(columnNumber);
            if (value == null) {
                return null;
            }
            return Splitter.on(",").splitToList(value);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
    @Qualifier
    public @interface Colon {}

    public static class ColonMapper implements ColumnMapper<List<String>> {
        @Override
        public List<String> map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
            String value = rs.getString(columnNumber);
            if (value == null) {
                return null;
            }
            return Splitter.on(":").splitToList(value);
        }
    }
}
