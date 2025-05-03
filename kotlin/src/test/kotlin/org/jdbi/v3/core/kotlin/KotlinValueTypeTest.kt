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
package org.jdbi.v3.core.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.argument.AbstractArgumentFactory
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class KotlinValueTypeTest {

    @RegisterExtension
    @JvmField
    val h2Extension: JdbiExtension = JdbiExtension.h2().withPlugin(KotlinPlugin())
        .withInitializer { ds, handle -> handle.execute("CREATE TABLE something (id integer, first text)") }

    private lateinit var handle: Handle

    @BeforeEach
    fun setup() {
        handle = h2Extension.sharedHandle
    }

    @JvmInline
    value class MagicType(val first: String)

    class MagicTypeArgumentFactory : AbstractArgumentFactory<MagicType>(Types.VARCHAR) {
        override fun build(value: MagicType?, config: ConfigRegistry): Argument? =
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

    @Test
    fun testValueClass() {
        handle.registerArgument(MagicTypeArgumentFactory())
        handle.registerColumnMapper(MagicTypeColumnMapper())
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

    data class TheThings(val id: Int, val first: MagicType)

    @Test
    fun testValueBean() {
        handle.registerArgument(MagicTypeArgumentFactory())
        handle.registerColumnMapper(MagicTypeColumnMapper())
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
}
