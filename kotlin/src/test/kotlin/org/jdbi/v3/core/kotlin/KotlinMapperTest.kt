package org.jdbi.v3.core.kotlin

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.HandleAccess
import org.jdbi.v3.core.mapper.reflect.ColumnName
import org.jdbi.v3.core.statement.StatementContextAccess
import org.junit.Before
import org.junit.Test
import java.sql.ResultSet
import java.sql.ResultSetMetaData

class KotlinMapperTest {


    val resultSet = mock<ResultSet>()
    val resultSetMetaData = mock<ResultSetMetaData>()
    val ctx = StatementContextAccess.createContext(HandleAccess.createHandle())

    private data class DataClassWithOnlyPrimaryConstructor(val id: Int, val name: String)

    @Before
    fun setUpMocks() {
        whenever(resultSet.metaData).thenReturn(resultSetMetaData)
    }

    @Test fun testDataClassWithOnlyPrimaryConstructor() {

        val mapper = KotlinMapper(DataClassWithOnlyPrimaryConstructor::class.java)
        mockColumns("id", "name")

        whenever(resultSet.getInt(1)).thenReturn(1)
        whenever(resultSet.getString(2)).thenReturn("one")
        whenever(resultSet.wasNull()).thenReturn(false)

        val thing = mapper.map(resultSet, ctx)

        assertThat(thing).isEqualToComparingFieldByField(DataClassWithOnlyPrimaryConstructor(1, "one"))

    }

    private data class DataClassWithAnnotatedParameter(val id: Int, @ColumnName("name") val n: String)

    @Test fun testDataClassWithAnnotatedParameter() {

        val mapper = KotlinMapper(DataClassWithAnnotatedParameter::class.java)
        mockColumns("id", "name")

        whenever(resultSet.getInt(1)).thenReturn(1)
        whenever(resultSet.getString(2)).thenReturn("one")
        whenever(resultSet.wasNull()).thenReturn(false)

        val thing = mapper.map(resultSet, ctx)

        assertThat(thing).isEqualToComparingFieldByField(DataClassWithAnnotatedParameter(1, "one"))

    }


    private class ClassWithOnlyPrimaryConstructor(val id: Int, val name: String)

    @Test fun testClassWithOnlyPrimaryConstructor() {

        val mapper = KotlinMapper(ClassWithOnlyPrimaryConstructor::class.java)
        mockColumns("id", "name")

        whenever(resultSet.getInt(1)).thenReturn(1)
        whenever(resultSet.getString(2)).thenReturn("one")
        whenever(resultSet.wasNull()).thenReturn(false)

        val thing = mapper.map(resultSet, ctx)

        assertThat(thing).isEqualToComparingFieldByField(ClassWithOnlyPrimaryConstructor(1, "one"))

    }


    private class ClassWithOnlySecondaryConstructor {
        val id: Int
        val name: String

        constructor(id: Int, name: String) {
            this.id = id
            this.name = name
        }
    }

    @Test fun testClassWithOnlySecondaryConstructor() {

        val mapper = KotlinMapper(ClassWithOnlySecondaryConstructor::class.java)
        mockColumns("id", "name")

        whenever(resultSet.getInt(1)).thenReturn(1)
        whenever(resultSet.getString(2)).thenReturn("one")
        whenever(resultSet.wasNull()).thenReturn(false)

        val thing = mapper.map(resultSet, ctx)

        assertThat(thing).isEqualToComparingFieldByField(ClassWithOnlySecondaryConstructor(1, "one"))

    }

    private class ClassWithWritableProperty(val id: Int, val name: String) {
        var foo: String = "foo"
    }

    @Test fun testClassWithWritableProperty() {

        val mapper = KotlinMapper(ClassWithWritableProperty::class.java)
        mockColumns("id", "name", "foo")

        whenever(resultSet.getInt(1)).thenReturn(1)
        whenever(resultSet.getString(2)).thenReturn("one")
        whenever(resultSet.getString(3)).thenReturn("bar")
        whenever(resultSet.wasNull()).thenReturn(false)

        val thing = mapper.map(resultSet, ctx)

        assertThat(thing).isEqualToComparingFieldByField(ClassWithWritableProperty(1, "one").apply { foo = "bar" })

    }

    private class ClassWithAnnotatedWritableProperty(val id: Int, val name: String) {
        @ColumnName("description")
        var foo: String = "foo"
    }

    @Test fun testClassWithAnnotatedWritableProperty() {

        val mapper = KotlinMapper(ClassWithAnnotatedWritableProperty::class.java)
        mockColumns("id", "name", "description")

        whenever(resultSet.getInt(1)).thenReturn(1)
        whenever(resultSet.getString(2)).thenReturn("one")
        whenever(resultSet.getString(3)).thenReturn("bar")
        whenever(resultSet.wasNull()).thenReturn(false)

        val thing = mapper.map(resultSet, ctx)

        assertThat(thing).isEqualToComparingFieldByField(ClassWithAnnotatedWritableProperty(1, "one").apply { foo = "bar" })

    }

    private fun mockColumns(vararg columns: String) {
        whenever(resultSetMetaData.columnCount).thenReturn(columns.size)
        for (i in columns.indices) {
            whenever(resultSetMetaData.getColumnLabel(i + 1)).thenReturn(columns[i])
            whenever(resultSet.findColumn(columns[i])).thenReturn(i + 1)
        }
    }

}