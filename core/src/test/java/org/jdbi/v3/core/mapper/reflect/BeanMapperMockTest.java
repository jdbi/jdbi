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

package org.jdbi.v3.core.mapper.reflect;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleAccess;
import org.jdbi.v3.core.SampleBean;
import org.jdbi.v3.core.ValueType;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.ValueTypeMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextAccess;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class BeanMapperMockTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withSomething();

    @Mock
    ResultSet resultSet;

    @Mock
    ResultSetMetaData resultSetMetaData;

    Handle handle = HandleAccess.createHandle();
    StatementContext ctx = StatementContextAccess.createContext(handle);

    RowMapper<SampleBean> mapper = BeanMapper.of(SampleBean.class);

    @Before
    public void setUpMocks() throws SQLException {
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    }

    private void mockColumns(String... columns) throws SQLException {
        when(resultSetMetaData.getColumnCount()).thenReturn(columns.length);
        for (int i = 0; i < columns.length; i++) {
            when(resultSetMetaData.getColumnLabel(i + 1)).thenReturn(columns[i]);
        }
    }

    private void mockLongResult(long aLong) throws SQLException {
        when(resultSet.getLong(1)).thenReturn(aLong);
        when(resultSet.wasNull()).thenReturn(false);
    }

    private void mockAllNullsResult() throws SQLException {
        when(resultSet.wasNull()).thenReturn(true);
    }

    @Test
    public void shouldSetValueOnPublicSetter() throws Exception {
        mockColumns("longField");

        Long aLongVal = 100L;
        mockLongResult(aLongVal);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);
    }

    @Test
    public void shouldHandleColumNameWithUnderscores() throws Exception {
        mockColumns("LONG_FIELD");

        Long aLongVal = 100L;
        mockLongResult(aLongVal);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);
    }

    @Test
    public void shouldBeCaseInSensitiveOfColumnWithUnderscoresAndPropertyNames() throws Exception {
        mockColumns("LoNg_FiElD");

        Long aLongVal = 100L;
        mockLongResult(aLongVal);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);
    }

    @Test
    public void shouldHandleEmptyResult() throws Exception {
        mockColumns();

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean).isNotNull();
    }

    @Test
    public void shouldBeCaseInSensitiveOfColumnAndPropertyNames() throws Exception {
        mockColumns("LoNgfielD");

        Long aLongVal = 100L;
        mockLongResult(aLongVal);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(aLongVal);
    }

    @Test
    public void shouldHandleNullValue() throws Exception {
        mockColumns("LoNgfielD");

        mockAllNullsResult();

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isNull();
    }

    @Test
    public void shouldSetValuesOnPublicSetter() throws Exception {
        mockColumns("longField");

        Long expected = 1L;
        mockLongResult(expected);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(expected);
    }

    @Test
    public void shouldThrowOnTotalMismatch() throws Exception {
        mockColumns("somethingElseEntirely");
        assertThatThrownBy(() -> mapper.map(resultSet, ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowOnProtectedSetter() throws Exception {
        mockColumns("protectedStringField");

        String expected = "string";
        when(resultSet.getString(1)).thenReturn(expected);
        when(resultSet.wasNull()).thenReturn(false);

        assertThatThrownBy(() -> mapper.map(resultSet, ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowOnPackagePrivateSetter() throws Exception {
        mockColumns("packagePrivateIntField");

        when(resultSet.getInt(1)).thenReturn(200);
        when(resultSet.wasNull()).thenReturn(false);

        assertThatThrownBy(() -> mapper.map(resultSet, ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowOnPrivateSetter() throws Exception {
        mockColumns("privateBigDecimalField");

        when(resultSet.getBigDecimal(1)).thenReturn(BigDecimal.ONE);
        when(resultSet.wasNull()).thenReturn(false);

        assertThatThrownBy(() -> mapper.map(resultSet, ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldSetValuesInSuperClassProperties() throws Exception {
        mockColumns("longField", "blongField");

        Long aLongVal = 100L;
        Long bLongVal = 200L;

        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.getLong(2)).thenReturn(bLongVal);
        when(resultSet.wasNull()).thenReturn(false);

        DerivedBean derivedBean = BeanMapper.of(DerivedBean.class).map(resultSet, ctx);

        assertThat(derivedBean.getLongField()).isEqualTo(aLongVal);
        assertThat(derivedBean.getBlongField()).isEqualTo(bLongVal);
    }

    @Test
    public void shouldUseRegisteredMapperForUnknownPropertyType() throws Exception {
        handle.registerColumnMapper(new ValueTypeMapper());

        mockColumns("longField", "valueTypeField");
        Long expected = 123L;

        when(resultSet.getLong(1)).thenReturn(expected);
        when(resultSet.getString(2)).thenReturn("foo");
        when(resultSet.wasNull()).thenReturn(false);

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(expected);
        assertThat(sampleBean.getValueTypeField()).isEqualTo(ValueType.valueOf("foo"));
    }

    @Test
    public void shouldThrowOnPropertyTypeWithoutRegisteredMapper() throws Exception {
        mockColumns("longField", "valueTypeField");

        when(resultSet.getLong(1)).thenReturn(123L);
        when(resultSet.getObject(2)).thenReturn(new Object());
        when(resultSet.wasNull()).thenReturn(false);

        assertThatThrownBy(() -> mapper.map(resultSet, ctx)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldNotThrowOnMismatchedColumns() throws Exception {
        mockColumns("longField", "extraColumn");

        Long expected = 666L;
        when(resultSet.getLong(1)).thenReturn(expected);
        when(resultSet.getString(2)).thenReturn("foo");

        SampleBean sampleBean = mapper.map(resultSet, ctx);

        assertThat(sampleBean.getLongField()).isEqualTo(expected);
    }

    @Test
    public void shouldThrowOnMismatchedColumnsStrictMatch() throws Exception {
        ctx.getConfig(ReflectionMappers.class).setStrictMatching(true);
        mockColumns("longField", "misspelledField");
        assertThatThrownBy(() -> mapper.map(resultSet, ctx)).isInstanceOf(IllegalArgumentException.class);
    }
}
