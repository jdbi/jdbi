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

import org.jdbi.v3.core.argument.AbstractArgumentFactory
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.sqlobject.SqlObject
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.jdbi.v3.testing.junit5.internal.TestingInitializers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.sql.Types

class TestListArgument {
    @RegisterExtension
    @JvmField
    val h2Extension: JdbiExtension = JdbiExtension.h2()
        .withInitializer(TestingInitializers.something())
        .withPlugin(KotlinSqlObjectPlugin())

    data class Something(val id: Int, val name: String)

    interface SomethingDao : SqlObject {
        @SqlQuery("select id, name from something")
        fun list(): List<Something>

        @SqlUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")
        fun insertList(id: Int, name: List<String>)

        @SqlUpdate("INSERT INTO something (id, name) VALUES (:id, :name)")
        fun inserValueList(id: Int, name: List<MagicValue>)
    }

    class StringListArgumentFactory : AbstractArgumentFactory<List<String>>(Types.VARCHAR) {
        override fun build(value: List<String>?, config: ConfigRegistry): Argument? = Argument { position, statement, _ ->
            if (value != null) {
                statement.setString(position, value.joinToString(","))
            } else {
                statement.setNull(position, Types.VARCHAR)
            }
        }
    }

    @Test
    fun testSingleInsert() {
        h2Extension.openHandle().use { handle ->
            handle.registerArgument(StringListArgumentFactory())
            val dao = handle.attach(SomethingDao::class)

            dao.insertList(1, listOf("one", "two"))

            val result = dao.list()
            assertEquals(1, result.size)
            assertEquals(Something(1, "one,two"), result[0])
        }
    }

    @JvmInline
    value class MagicValue(val value: String)

    class MagicListArgumentFactory : AbstractArgumentFactory<List<MagicValue>>(Types.VARCHAR) {
        override fun build(value: List<MagicValue>?, config: ConfigRegistry): Argument? = Argument { position, statement, _ ->
            if (value != null) {
                statement.setString(position, value.joinToString(",") { x -> x.value })
            } else {
                statement.setNull(position, Types.VARCHAR)
            }
        }
    }

    @Test
    fun testSingleInsertWithMagicValue() {
        h2Extension.openHandle().use { handle ->
            handle.registerArgument(MagicListArgumentFactory())
            val dao = handle.attach(SomethingDao::class)

            dao.inserValueList(1, listOf(MagicValue("one"), MagicValue("two")))

            val result = dao.list()
            assertEquals(1, result.size)
            assertEquals(Something(1, "one,two"), result[0])
        }
    }
}
