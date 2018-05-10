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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleAccess;
import org.jdbi.v3.core.SampleImmutable;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextAccess;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ImmutableTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    @Mock
    ResultSet resultSet;

    @Mock
    ResultSetMetaData resultSetMetaData;

    Handle handle = HandleAccess.createHandle();
    StatementContext ctx = StatementContextAccess.createContext(handle);

    RowMapper<SampleImmutable> mapper = ImmutableMapper.of(SampleImmutable.class);

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

    private void mockAllNullsResult() throws SQLException {
        when(resultSet.wasNull()).thenReturn(true);
    }

    @Test
    public void shouldSetValueWithBuilder() throws Exception {
        int aIntVal1 = 42;
        long aLongVal = 100L;
        String aStringValue = "Foo";
        int aIntVal2 = 42;

        mockColumns("id", "name", "valueInt", "inner_id");

        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.getString(2)).thenReturn(aStringValue);
        when(resultSet.getInt(3)).thenReturn(aIntVal1);
        when(resultSet.getInt(4)).thenReturn(aIntVal2);
        when(resultSet.wasNull()).thenReturn(false);

        SampleImmutable sampleImmutable = mapper.map(resultSet, ctx);

        assertThat(sampleImmutable.id()).isEqualTo(aLongVal);
        assertThat(sampleImmutable.getName()).isEqualTo(aStringValue);
        assertThat(sampleImmutable.valueInt()).isEqualTo(aIntVal1);
        assertThat(sampleImmutable.inner().id()).isEqualTo(aIntVal2);
    }

    @Test(expected = IllegalStateException.class)
    public void expectedValueNotSet() throws Exception {
        mockColumns("id", "valueInt", "inner_id");

        when(resultSet.getLong(1)).thenReturn(100L);
        when(resultSet.getInt(2)).thenReturn(42);
        when(resultSet.getInt(3)).thenReturn(69);

        mapper.map(resultSet, ctx);
    }

    @Test
    public void expectedValueNotSetWithDefault() throws Exception {
        String aStringValue = "Foo";
        int aIntVal1 = 42;
        int aIntVal2 = 42;

        mockColumns("name", "valueInt", "inner_id");

        when(resultSet.getString(1)).thenReturn(aStringValue);
        when(resultSet.getInt(2)).thenReturn(aIntVal1);
        when(resultSet.getInt(3)).thenReturn(aIntVal2);
        when(resultSet.wasNull()).thenReturn(false);

        SampleImmutable sampleImmutable = mapper.map(resultSet, ctx);

        assertThat(sampleImmutable.id()).isEqualTo(0);
        assertThat(sampleImmutable.valueInt()).isEqualTo(aIntVal1);
        assertThat(sampleImmutable.getName()).isEqualTo(aStringValue);
        assertThat(sampleImmutable.inner().id()).isEqualTo(aIntVal2);
    }

    @Test
    public void shouldHandleColumNameWithUnderscores() throws Exception {
        int aIntVal1 = 42;
        long aLongVal = 100L;
        String aStringValue = "Foo";
        int aIntVal2 = 42;

        mockColumns("id", "name", "value_int", "inner_id");

        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.getString(2)).thenReturn(aStringValue);
        when(resultSet.getInt(3)).thenReturn(aIntVal1);
        when(resultSet.getInt(4)).thenReturn(aIntVal2);
        when(resultSet.wasNull()).thenReturn(false);

        SampleImmutable sampleImmutable = mapper.map(resultSet, ctx);

        assertThat(sampleImmutable.id()).isEqualTo(aLongVal);
        assertThat(sampleImmutable.getName()).isEqualTo(aStringValue);
        assertThat(sampleImmutable.valueInt()).isEqualTo(aIntVal1);
        assertThat(sampleImmutable.inner().id()).isEqualTo(aIntVal2);
    }

    @Test
    public void shouldBeCaseInSensitiveOfColumnWithUnderscoresAndPropertyNames() throws Exception {
        int aIntVal = 42;
        long aLongVal = 100L;
        String aStringValue = "Foo";
        int aIntVal2 = 42;

        mockColumns("id", "name", "VaLUe_iNt", "inner_id");

        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.getString(2)).thenReturn(aStringValue);
        when(resultSet.getInt(3)).thenReturn(aIntVal);
        when(resultSet.getInt(4)).thenReturn(aIntVal2);
        when(resultSet.wasNull()).thenReturn(false);

        SampleImmutable sampleImmutable = mapper.map(resultSet, ctx);

        assertThat(sampleImmutable.id()).isEqualTo(aLongVal);
        assertThat(sampleImmutable.getName()).isEqualTo(aStringValue);
        assertThat(sampleImmutable.valueInt()).isEqualTo(aIntVal);
        assertThat(sampleImmutable.inner().id()).isEqualTo(aIntVal2);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldntHandleEmptyResult() throws Exception {
        mockColumns();

        mapper.map(resultSet, ctx);
    }
}
