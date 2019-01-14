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
package org.jdbi.v3.sqlobject;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestBindMethods {

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Before
    public void setUp() {
        final Handle h = this.dbRule.getSharedHandle();
        h.execute("CREATE TABLE bind_methods (id IDENTITY PRIMARY KEY, value BIGINT)");
    }

    @Test
    public void testBindMethodsDirect() {
        final PairRowDAO dao = this.dbRule.getJdbi().onDemand(PairRowDAO.class);
        final long testValue = 709L;
        final int testId = 5;

        final DatabaseValue<?> dbVal = new DirectDatabaseLongValue(testValue);
        final PairRow row = new PairRow(testId, dbVal);

        assertThat(dao.insert(row)).isEqualTo(1);
        assertThat(dao.getById(testId)).isEqualTo(testValue);
    }

    @Test
    public void testBindMethodsImplicitOverride() {
        final PairRowDAO dao = this.dbRule.getJdbi().onDemand(PairRowDAO.class);
        final long testValue = 708L;
        final int testId = 6;

        final DatabaseValue<?> dbVal = new DatabaseLongValueWithOverride(testValue);
        final PairRow row = new PairRow(testId, dbVal);

        assertThat(dao.insert(row)).isEqualTo(1);
        assertThat(dao.getById(testId)).isEqualTo(testValue);
    }

    @Test
    public void testBindMethodsImplicit() {
        final PairRowDAO dao = this.dbRule.getJdbi().onDemand(PairRowDAO.class);
        final long testValue = 707L;
        final int testId = 7;

        final DatabaseValue<?> dbVal = new DatabaseLongValue(testValue);
        final PairRow row = new PairRow(testId, dbVal);

        assertThat(dao.insert(row)).isEqualTo(1);
        assertThat(dao.getById(testId)).isEqualTo(testValue);
    }

    @Test
    public void testSanity() {
        final String methodName = "getColumnValue";

        // Only one bridge method expected.
        final List<Method> direct = Arrays.stream(DirectDatabaseLongValue.class.getMethods())
            .filter(m -> m.getParameterCount() == 0 && m.getName().equals(methodName))
            .collect(Collectors.toList());

        assertThat(direct.size()).isEqualTo(2);
        assertThat(direct.stream().filter(m -> m.isBridge())).hasSize(1);

        // This version has multiple bridge methods!
        final List<Method> override = Arrays.stream(DatabaseLongValueWithOverride.class.getMethods())
            .filter(m -> m.getParameterCount() == 0 && m.getName().equals(methodName))
            .collect(Collectors.toList());

        assertThat(override.size()).isEqualTo(3);
        assertThat(override.stream().filter(m -> m.isBridge())).hasSize(2);

        // Only one bridge method expected.
        final List<Method> implicit = Arrays.stream(DatabaseLongValue.class.getMethods())
            .filter(m -> m.getParameterCount() == 0 && m.getName().equals(methodName))
            .collect(Collectors.toList());

        assertThat(implicit.size()).isEqualTo(2);
        assertThat(implicit.stream().filter(m -> m.isBridge())).hasSize(1);
    }

    public interface PairRowDAO {

        @SqlUpdate("INSERT INTO bind_methods (id, value) VALUES(:row.getKey, :row.getValue.getColumnValue)")
        @RegisterArgumentFactory(BigIntNumberArgumentFactory.class)
        int insert(@BindMethods("row") PairRow pairRow);

        @SqlQuery("SELECT value FROM bind_methods WHERE id = :id")
        Long getById(@Bind("id") int dbid);
    }

    public interface DatabaseValue<T> {

        T getColumnValue();
    }

    public static final class PairRow {

        private final int id;
        private final DatabaseValue<?> value;

        PairRow(final int id, final DatabaseValue<?> value) {
            super();
            this.id = id;
            this.value = value;
        }

        public int getKey() {
            return this.id;
        }

        public DatabaseValue<?> getValue() {
            return this.value;
        }
    }

    public static class DatabaseNumberValue<T extends Number> implements DatabaseValue<T> {

        private final T value;

        DatabaseNumberValue(final T value) {
            super();
            this.value = value;
        }

        @Override
        public T getColumnValue() {
            return this.value;
        }
    }

    public static final class DatabaseLongValue extends DatabaseNumberValue<Long> {

        DatabaseLongValue(final long value) {
            super(Long.valueOf(value));
        }
    }

    public static final class DatabaseLongValueWithOverride extends DatabaseNumberValue<Long> {

        DatabaseLongValueWithOverride(final long value) {
            super(Long.valueOf(value));
        }

        @Override
        public Long getColumnValue() {
            return super.getColumnValue();
        }
    }

    public static final class DirectDatabaseLongValue implements DatabaseValue<Long> {

        private final long value;

        DirectDatabaseLongValue(final long value) {
            super();
            this.value = value;
        }

        @Override
        public Long getColumnValue() {
            return Long.valueOf(this.value);
        }
    }

    public static final class BigIntNumberArgument implements Argument {

        private final Number value;

        public BigIntNumberArgument(final Number value) {
            this.value = value;
        }

        @Override
        public void apply(final int position, final PreparedStatement statement, final StatementContext ctx) throws SQLException {
            statement.setLong(position, this.value.longValue());
        }

        @Override
        public String toString() {
            return Objects.toString(this.value);
        }
    }

    public static final class BigIntNumberArgumentFactory extends AbstractArgumentFactory<Number> {

        public BigIntNumberArgumentFactory() {
            super(Types.BIGINT);
        }

        @Override
        protected Argument build(final Number value, final ConfigRegistry config) {
            return new BigIntNumberArgument(value);
        }
    }
}
