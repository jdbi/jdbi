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
        int aIntVal = 42;
        long aLongVal = 100L;
        String aStringValue = "Foo";

        mockColumns("id", "name", "valueInt");

        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.getString(2)).thenReturn(aStringValue);
        when(resultSet.getInt(3)).thenReturn(aIntVal);
        when(resultSet.wasNull()).thenReturn(false);

        SampleImmutable sampleImmutable = mapper.map(resultSet, ctx);

        assertThat(sampleImmutable.id()).isEqualTo(aLongVal);
        assertThat(sampleImmutable.name()).isEqualTo(aStringValue);
        assertThat(sampleImmutable.valueInt()).isEqualTo(aIntVal);
    }

    @Test(expected = IllegalStateException.class)
    public void expectedValueNotSet() throws Exception {
        long aLongVal = 100L;
        int aIntVal = 42;

        mockColumns("id", "valueInt");

        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.getInt(2)).thenReturn(aIntVal);

        mapper.map(resultSet, ctx);
    }

    @Test
    public void expectedValueNotSetWithDefault() throws Exception {
        String aStringValue = "Foo";
        int aIntVal = 42;

        mockColumns("name", "valueInt");

        when(resultSet.getString(1)).thenReturn(aStringValue);
        when(resultSet.getInt(2)).thenReturn(aIntVal);
        when(resultSet.wasNull()).thenReturn(false);

        SampleImmutable sampleImmutable = mapper.map(resultSet, ctx);

        assertThat(sampleImmutable.id()).isEqualTo(0);
        assertThat(sampleImmutable.valueInt()).isEqualTo(aIntVal);
        assertThat(sampleImmutable.name()).isEqualTo(aStringValue);
    }

    @Test
    public void shouldHandleColumNameWithUnderscores() throws Exception {
        int aIntVal = 42;
        long aLongVal = 100L;
        String aStringValue = "Foo";

        mockColumns("id", "name", "value_int");

        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.getString(2)).thenReturn(aStringValue);
        when(resultSet.getInt(3)).thenReturn(aIntVal);
        when(resultSet.wasNull()).thenReturn(false);

        SampleImmutable sampleImmutable = mapper.map(resultSet, ctx);

        assertThat(sampleImmutable.id()).isEqualTo(aLongVal);
        assertThat(sampleImmutable.name()).isEqualTo(aStringValue);
        assertThat(sampleImmutable.valueInt()).isEqualTo(aIntVal);
    }

    @Test
    public void shouldBeCaseInSensitiveOfColumnWithUnderscoresAndPropertyNames() throws Exception {
        int aIntVal = 42;
        long aLongVal = 100L;
        String aStringValue = "Foo";

        mockColumns("id", "name", "VaLUe_iNt");

        when(resultSet.getLong(1)).thenReturn(aLongVal);
        when(resultSet.getString(2)).thenReturn(aStringValue);
        when(resultSet.getInt(3)).thenReturn(aIntVal);
        when(resultSet.wasNull()).thenReturn(false);

        SampleImmutable sampleImmutable = mapper.map(resultSet, ctx);

        assertThat(sampleImmutable.id()).isEqualTo(aLongVal);
        assertThat(sampleImmutable.name()).isEqualTo(aStringValue);
        assertThat(sampleImmutable.valueInt()).isEqualTo(aIntVal);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldntHandleEmptyResult() throws Exception {
        mockColumns();

        mapper.map(resultSet, ctx);
    }
}
