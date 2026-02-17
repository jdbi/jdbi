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
package org.jdbi.core.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.jdbi.core.Handle
import org.jdbi.core.argument.AbstractArgumentFactory
import org.jdbi.core.argument.Argument
import org.jdbi.core.argument.ArgumentFactory
import org.jdbi.core.config.ConfigRegistry
import org.jdbi.core.kotlin.internal.KotlinValueClassArgumentFactory
import org.jdbi.core.kotlin.internal.KotlinValueClassColumnMapperFactory
import org.jdbi.core.mapper.ColumnMapper
import org.jdbi.core.mapper.ColumnMapperFactory
import org.jdbi.core.statement.StatementContext
import org.jdbi.testing.junit5.JdbiExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.lang.reflect.Type
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.Optional
import java.util.stream.Stream

class KotlinValueTypeTest {

    @RegisterExtension
    @JvmField
    val h2Extension: JdbiExtension = JdbiExtension.h2().withPlugin(KotlinPlugin(installKotlinMapperFactory = false))
        .withInitializer { ds, handle -> handle.execute("CREATE TABLE something (id integer, first text, other text)") }

    private lateinit var handle: Handle

    @BeforeEach
    fun setup() {
        handle = h2Extension.sharedHandle
    }

    @JvmInline
    value class MagicType(val first: String)

    class MagicTypeArgumentFactory : AbstractArgumentFactory<MagicType>(Types.VARCHAR) {
        override fun build(value: MagicType?, config: ConfigRegistry): Argument =
            Argument { position: Int, statement: PreparedStatement, ctx: StatementContext ->
                if (value != null) {
                    statement.setString(position, value.first)
                } else {
                    statement.setNull(position, Types.VARCHAR)
                }
            }
    }

    class MagicTypeColumnMapper : ColumnMapper<MagicType?> {
        override fun map(rs: ResultSet, columnNumber: Int, ctx: StatementContext): MagicType? {
            val value = rs.getString(columnNumber)
            return if (rs.wasNull()) null else MagicType(value)
        }
    }

    class MagicTypeColumnMapperFactory : ColumnMapperFactory {
        override fun build(type: Type?, config: ConfigRegistry?): Optional<ColumnMapper<*>> {
            if (type == MagicType::class.java) {
                return Optional.of(MagicTypeColumnMapper())
            }
            return Optional.empty()
        }
    }

    companion object {
        @JvmStatic
        fun arguments(): Stream<Arguments> {
            val mappers = listOf(
                KotlinValueClassColumnMapperFactory(), // Kotlin Value class support
                MagicTypeColumnMapperFactory()
            )
            val argumentFactories = listOf(
                KotlinValueClassArgumentFactory(), // Kotlin Value class support
                MagicTypeArgumentFactory()
            )

            val arguments = ArrayList<Arguments>()

            for (mapper in mappers) {
                for (argumentFactory in argumentFactories) {
                    arguments.add(Arguments.of(mapper, argumentFactory))
                }
            }

            return arguments.stream()
        }
    }

    @ParameterizedTest
    @MethodSource("arguments")
    fun testValueClass(columnMapperFactory: ColumnMapperFactory, argumentFactory: ArgumentFactory) {
        handle.registerArgument(argumentFactory)
        handle.registerColumnMapper(columnMapperFactory)
        val expected = MagicType("does this work?")

        handle.createUpdate("INSERT INTO something(id, first) VALUES(:id, :first)")
            .bind("id", 1)
            .bind("first", expected)
            .execute()

        val result = handle.createQuery("SELECT first FROM something")
            .mapTo<MagicType>()
            .single()

        assertThat(result).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("arguments")
    fun testValueClassList(columnMapperFactory: ColumnMapperFactory, argumentFactory: ArgumentFactory) {
        handle.registerArgument(argumentFactory)
        handle.registerColumnMapper(columnMapperFactory)
        val expected = MagicType("does this work?")

        for (i in 1..10) {
            handle.createUpdate("INSERT INTO something(id, first) VALUES(:id, :first)")
                .bind("id", i)
                .bind("first", expected)
                .execute()
        }

        val result = handle.createQuery("SELECT first FROM something ORDER BY id")
            .mapTo<MagicType>()
            .list()

        assertThat(result).hasSize(10)
        for (i in 0..9) {
            assertThat(result.get(i)).isEqualTo(expected)
        }
    }

    @ParameterizedTest
    @MethodSource("arguments")
    fun testValueClassNull(columnMapperFactory: ColumnMapperFactory, argumentFactory: ArgumentFactory) {
        handle.registerArgument(argumentFactory)
        handle.registerColumnMapper(columnMapperFactory)

        handle.createUpdate("INSERT INTO something(id, first) VALUES(:id, :first)")
            .bind("id", 1)
            .bindNull("first", Types.VARCHAR)
            .execute()

        val result = handle.createQuery("SELECT first FROM something")
            .mapTo<MagicType>()
            .single()

        assertThat(result).isNull()
    }

    data class TheThings(val id: Int, val first: MagicType)

    @ParameterizedTest
    @MethodSource("arguments")
    fun testValueBean(columnMapperFactory: ColumnMapperFactory, argumentFactory: ArgumentFactory) {
        handle.registerArgument(argumentFactory)
        handle.registerColumnMapper(columnMapperFactory)

        // Not automatically registered b/c using installKotlinMapperFactory = false
        handle.registerRowMapper(KotlinMapperFactory())

        val expected = MagicType("does this work?")

        handle.createUpdate("INSERT INTO something(id, first) VALUES(:id, :first)")
            .bind("id", 1)
            .bind("first", expected)
            .execute()

        val result = handle.createQuery("SELECT id, first from something")
            .mapTo<TheThings>()
            .single()

        assertThat(result).isEqualTo(TheThings(1, expected))
    }

    @ParameterizedTest
    @MethodSource("arguments")
    fun testValueBeanList(columnMapperFactory: ColumnMapperFactory, argumentFactory: ArgumentFactory) {
        handle.registerArgument(argumentFactory)
        handle.registerColumnMapper(columnMapperFactory)

        // Not automatically registered b/c using installKotlinMapperFactory = false
        handle.registerRowMapper(KotlinMapperFactory())

        val expected = MagicType("does this work?")

        for (i in 1..10) {
            handle.createUpdate("INSERT INTO something(id, first) VALUES(:id, :first)")
                .bind("id", i)
                .bind("first", expected)
                .execute()
        }

        val result = handle.createQuery("SELECT id, first from something")
            .mapTo<TheThings>()
            .list()

        assertThat(result).hasSize(10)
        for (i in 0..9) {
            assertThat(result.get(i))
                .hasFieldOrPropertyWithValue("id", i + 1)
                .hasFieldOrPropertyWithValue("first", expected.first) // someone brought a java library to a kotlin test...
        }
    }

    @JvmInline
    value class SpaceType(val value: String)

    data class MoreThings(val id: Int, val first: MagicType, val other: SpaceType)

    @Test
    fun testMultiValueClasses() {
        handle.registerArgument(KotlinValueClassArgumentFactory())
        handle.registerColumnMapper(KotlinValueClassColumnMapperFactory())
        handle.registerRowMapper(KotlinMapperFactory())

        val expected = MagicType("does this work?")
        val other = SpaceType("no it does not.")
        val ktBean = MoreThings(1, expected, other)

        handle.createUpdate("INSERT INTO something(id, first, other) VALUES(:id, :first, :other)")
            .bindKotlin(ktBean)
            .execute()

        val result = handle.createQuery("SELECT id, first, other from something")
            .mapTo<MoreThings>()
            .single()

        assertThat(result).isEqualTo(MoreThings(1, expected, other))
    }
}
