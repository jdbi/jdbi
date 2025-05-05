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
package org.jdbi.v3.sqlobject.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.argument.AbstractArgumentFactory
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.kotlin.KotlinMapper
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.jdbi.v3.testing.junit5.internal.TestingInitializers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.stream.Stream

/**
 * Test a value class as a parameter for a sqlobject method - see https://github.com/jdbi/jdbi/issues/2790
 */
class TestValueClasses {
    @RegisterExtension
    var h2Extension: JdbiExtension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(SqlObjectPlugin())

    private lateinit var handle: Handle
    private lateinit var dao: Dao

    @BeforeEach
    fun setUp() {
        handle = h2Extension.jdbi.open()
        handle.registerArgument(ValueIdArgumentFactory())
        handle.registerColumnMapper(ValueId::class.java, ValueIdColumnMapper())
        dao = handle.attach(Dao::class)
    }

    @AfterEach
    fun tearDown() {
        handle.close()
    }

    @JvmInline
    value class ValueId(val value: Int)

    @Test
    fun testValueClassParameters() {
        dao.insert(ValueId(2), "Bean")
        assertThat(dao.findNameById(ValueId(2))).isEqualTo("Bean")
    }

    companion object {
        @JvmStatic
        fun mapperProvider(): Stream<RowMapper<*>> = Stream.of(SomethingMapper(), KotlinMapper(Something::class))
    }

    @ParameterizedTest
    @MethodSource("mapperProvider")
    fun testMapper(mapper: RowMapper<*>) {
        handle.registerRowMapper(Something::class.java, mapper)
        handle.execute("insert into something (id, name) values (6, 'Martin')")
        // can't use the dao; it does not have the row mapper registered
        val dao2 = handle.attach(Dao::class)
        val s = dao2.retrieveById(ValueId(6))
        assertThat(s.name).isEqualTo("Martin")
    }

    @ParameterizedTest
    @MethodSource("mapperProvider")
    fun testListMapper(mapper: RowMapper<*>) {
        handle.registerRowMapper(Something::class.java, mapper)
        dao.insert(ValueId(2), "Bean")
        dao.insert(ValueId(6), "Martin")
        // can't use the dao; it does not have the row mapper registered
        val dao2 = handle.attach(Dao::class)
        val s = dao2.loadAll()
        assertThat(s).hasSize(2)
        assertThat(s).containsExactly(Something(ValueId(2), "Bean"), Something(ValueId(6), "Martin"))
    }

    @ParameterizedTest
    @MethodSource("mapperProvider")
    fun testValueClassListMapper(mapper: RowMapper<*>) {
        handle.registerRowMapper(Something::class.java, mapper)
        dao.insert(ValueId(2), "Bean")
        dao.insert(ValueId(6), "Martin")
        // can't use the dao; it does not have the row mapper registered
        val dao2 = handle.attach(Dao::class)
        val s = dao2.loadAllIds()
        assertThat(s).hasSize(2)
        assertThat(s).containsExactly(ValueId(2), ValueId(6))
    }

    @ParameterizedTest
    @MethodSource("mapperProvider")
    fun testRegularListMapper(mapper: RowMapper<*>) {
        handle.registerRowMapper(Something::class.java, mapper)
        dao.insert(ValueId(2), "Bean")
        dao.insert(ValueId(6), "Martin")
        // can't use the dao; it does not have the row mapper registered
        val dao2 = handle.attach(Dao::class)
        val s = dao2.loadAllNames()
        assertThat(s).hasSize(2)
        assertThat(s).containsExactly("Bean", "Martin")
    }

    @Test
    fun testValueClassColumn() {
        dao.insert(ValueId(6), "Martin")

        val s = dao.findIdByName("Martin")
        assertThat(s).isEqualTo(ValueId(6))

        val t = dao.findIdByName("Brian")
        assertThat(t).isNull()
    }

    @UseStringTemplateSqlLocator
    interface Dao {
        @SqlUpdate("insert")
        fun insert(@Bind("id") id: ValueId, @Bind("name") name: String): Int

        // A value class as a parameter requires an explicit value for the SQL query; Kotlin mangles the method name (findNameById-<xxxx>) otherwise.
        @SqlQuery("findNameById")
        fun findNameById(@Bind("id") id: ValueId): String?

        @SqlQuery("retrieveById")
        fun retrieveById(@Bind("id") id: ValueId): Something

        @SqlQuery("findIdByName")
        fun findIdByName(@Bind("name") name: String?): ValueId?

        @SqlQuery("loadAll")
        fun loadAll(): List<Something>

        @SqlQuery("loadAllIds")
        fun loadAllIds(): List<ValueId>

        @SqlQuery("loadAllNames")
        fun loadAllNames(): List<String>
    }

    class SomethingMapper : RowMapper<Something> {
        @Throws(SQLException::class)
        override fun map(r: ResultSet, ctx: StatementContext): Something = Something(ValueId(r.getInt("id")), r.getString("name"))
    }

    data class Something(val id: ValueId, val name: String)

    class ValueIdArgumentFactory : AbstractArgumentFactory<ValueId>(Types.INTEGER) {
        override fun build(value: ValueId?, config: ConfigRegistry): Argument? =
            Argument { position: Int, statement: PreparedStatement, ctx: StatementContext ->
                if (value != null) {
                    statement.setInt(position, value.value)
                } else {
                    statement.setNull(position, Types.INTEGER)
                }
            }
    }

    // value type as a column

    class ValueIdColumnMapper : ColumnMapper<ValueId?> {
        override fun map(rs: ResultSet, columnNumber: Int, ctx: StatementContext): ValueId? {
            val value = rs.getInt(columnNumber)
            return if (rs.wasNull()) {
                null
            } else {
                ValueId(value)
            }
        }
    }
}
