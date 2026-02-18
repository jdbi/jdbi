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
package org.jdbi.sqlobject.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.jdbi.core.Handle
import org.jdbi.core.argument.AbstractArgumentFactory
import org.jdbi.core.argument.Argument
import org.jdbi.core.argument.ArgumentFactory
import org.jdbi.core.config.ConfigRegistry
import org.jdbi.core.kotlin.KotlinMapper
import org.jdbi.core.kotlin.internal.KotlinValueClassArgumentFactory
import org.jdbi.core.kotlin.internal.KotlinValueClassColumnMapperFactory
import org.jdbi.core.mapper.ColumnMapper
import org.jdbi.core.mapper.ColumnMapperFactory
import org.jdbi.core.mapper.RowMapper
import org.jdbi.core.statement.StatementContext
import org.jdbi.sqlobject.customizer.Bind
import org.jdbi.sqlobject.statement.SqlQuery
import org.jdbi.sqlobject.statement.SqlUpdate
import org.jdbi.stringtemplate4.UseStringTemplateSqlLocator
import org.jdbi.testing.junit.JdbiExtension
import org.jdbi.testing.junit.internal.TestingInitializers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.lang.reflect.Type
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.Optional
import java.util.stream.Stream

/**
 * Test a value class as a parameter for a sqlobject method - see https://github.com/jdbi/jdbi/issues/2790
 */
class TestValueClasses {
    @RegisterExtension
    var h2Extension: JdbiExtension = JdbiExtension.h2()
        .withInitializer(TestingInitializers.something())
        .withPlugin(KotlinSqlObjectPlugin(installKotlinMapperFactory = false))

    private lateinit var handle: Handle

    @BeforeEach
    fun setUp() {
        handle = h2Extension.jdbi.open()
    }

    @AfterEach
    fun tearDown() {
        handle.close()
    }

    @JvmInline
    value class ValueId(val value: Int)

    @Test
    fun testValueClassParameters() {
        val dao = handle.attach(Dao::class)
        dao.insert(ValueId(2), "Bean")
        assertThat(dao.findNameById(ValueId(2))).isEqualTo("Bean")
    }

    companion object {
        @JvmStatic
        fun arguments(): Stream<Arguments> {
            val colMappers = listOf(
                KotlinValueClassColumnMapperFactory(), // Kotlin Value class support
                ValueIdColumnMapperFactory()
            )
            val argumentFactories = listOf(
                KotlinValueClassArgumentFactory(), // Kotlin Value class support
                ValueIdArgumentFactory()
            )

            val rowMappers = listOf(
                SomethingMapper(),
                KotlinMapper(Something::class)
            )

            val arguments = ArrayList<Arguments>()

            for (colMapper in colMappers) {
                for (argumentFactory in argumentFactories) {
                    for (rowMapper in rowMappers) {
                        arguments.add(Arguments.of(colMapper, argumentFactory, rowMapper))
                    }
                }
            }

            return arguments.stream()
        }
    }

    @ParameterizedTest
    @MethodSource("arguments")
    fun testMapper(columnMapperFactory: ColumnMapperFactory, argumentFactory: ArgumentFactory, rowMapper: RowMapper<*>) {
        handle.registerRowMapper(Something::class.java, rowMapper)
        handle.registerColumnMapper(columnMapperFactory)
        handle.registerArgument(argumentFactory)

        handle.execute("insert into something (id, name) values (6, 'Martin')")

        val dao = handle.attach(Dao::class)
        val s = dao.retrieveById(ValueId(6))
        assertThat(s.name).isEqualTo("Martin")
    }

    @ParameterizedTest
    @MethodSource("arguments")
    fun testListMapper(columnMapperFactory: ColumnMapperFactory, argumentFactory: ArgumentFactory, rowMapper: RowMapper<*>) {
        handle.registerRowMapper(Something::class.java, rowMapper)
        handle.registerColumnMapper(columnMapperFactory)
        handle.registerArgument(argumentFactory)

        val dao = handle.attach(Dao::class)

        dao.insert(ValueId(2), "Bean")
        dao.insert(ValueId(6), "Martin")

        val s = dao.loadAll()
        assertThat(s).hasSize(2)
        assertThat(s).containsExactly(Something(ValueId(2), "Bean"), Something(ValueId(6), "Martin"))
    }

    @ParameterizedTest
    @MethodSource("arguments")
    fun testListMapperForJava(columnMapperFactory: ColumnMapperFactory, argumentFactory: ArgumentFactory, rowMapper: RowMapper<*>) {
        handle.registerRowMapper(Something::class.java, rowMapper)
        handle.registerColumnMapper(columnMapperFactory)
        handle.registerArgument(argumentFactory)

        val dao = handle.attach(Dao::class)

        dao.insert(ValueId(2), "Bean")
        dao.insert(ValueId(6), "Martin")

        val s: java.util.List<Something> = dao.loadAllJava()
        assertThat(s).hasSize(2)
        assertThat(s).containsExactly(Something(ValueId(2), "Bean"), Something(ValueId(6), "Martin"))
    }

    @ParameterizedTest
    @MethodSource("arguments")
    fun testValueClassListMapper(columnMapperFactory: ColumnMapperFactory, argumentFactory: ArgumentFactory, rowMapper: RowMapper<*>) {
        handle.registerRowMapper(Something::class.java, rowMapper)
        handle.registerColumnMapper(columnMapperFactory)
        handle.registerArgument(argumentFactory)

        val dao = handle.attach(Dao::class)

        dao.insert(ValueId(2), "Bean")
        dao.insert(ValueId(6), "Martin")

        val s = dao.loadAllIds()
        assertThat(s).hasSize(2)
        assertThat(s).containsExactly(ValueId(2), ValueId(6))
    }

    @ParameterizedTest
    @MethodSource("arguments")
    fun testValueClassListMapperForJava(columnMapperFactory: ColumnMapperFactory, argumentFactory: ArgumentFactory, rowMapper: RowMapper<*>) {
        handle.registerRowMapper(Something::class.java, rowMapper)
        handle.registerColumnMapper(columnMapperFactory)
        handle.registerArgument(argumentFactory)

        val dao = handle.attach(Dao::class)

        dao.insert(ValueId(2), "Bean")
        dao.insert(ValueId(6), "Martin")

        val s: java.util.List<ValueId> = dao.loadAllIdsJava()
        assertThat(s).hasSize(2)
        assertThat(s).containsExactly(ValueId(2), ValueId(6))
    }

    @ParameterizedTest
    @MethodSource("arguments")
    fun testRegularListMapper(columnMapperFactory: ColumnMapperFactory, argumentFactory: ArgumentFactory, rowMapper: RowMapper<*>) {
        handle.registerRowMapper(Something::class.java, rowMapper)
        handle.registerColumnMapper(columnMapperFactory)
        handle.registerArgument(argumentFactory)

        val dao = handle.attach(Dao::class)

        dao.insert(ValueId(2), "Bean")
        dao.insert(ValueId(6), "Martin")

        val s = dao.loadAllNames()
        assertThat(s).hasSize(2)
        assertThat(s).containsExactly("Bean", "Martin")
    }

    @Test
    fun testValueClassColumn() {
        handle.registerColumnMapper(ValueId::class.java, ValueIdColumnMapper())

        val dao = handle.attach(Dao::class)

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

        @SqlQuery("loadAll")
        fun loadAllJava(): java.util.List<Something>

        @SqlQuery("loadAllIds")
        fun loadAllIdsJava(): java.util.List<ValueId>
    }

    class SomethingMapper : RowMapper<Something> {
        @Throws(SQLException::class)
        override fun map(r: ResultSet, ctx: StatementContext): Something = Something(ValueId(r.getInt("id")), r.getString("name"))

        override fun toString(): String = "SomethingMapper"
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

        override fun toString(): String = "ValueIdArgumentFactory"
    }

    // value type as a column

    class ValueIdColumnMapperFactory : ColumnMapperFactory {
        override fun build(type: Type?, config: ConfigRegistry?): Optional<ColumnMapper<*>> {
            if (type == ValueId::class.java) {
                return Optional.of(ValueIdColumnMapper())
            }
            return Optional.empty()
        }

        override fun toString(): String = "ValueIdColumnMapperFactory"
    }

    class ValueIdColumnMapper : ColumnMapper<ValueId?> {
        override fun map(rs: ResultSet, columnNumber: Int, ctx: StatementContext): ValueId? {
            val value = rs.getInt(columnNumber)
            return if (rs.wasNull()) {
                null
            } else {
                ValueId(value)
            }
        }

        override fun toString(): String = "ValueIdColumnMapper"
    }
}
