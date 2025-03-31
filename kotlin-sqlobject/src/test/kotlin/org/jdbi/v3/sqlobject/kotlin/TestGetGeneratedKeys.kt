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

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.useExtension
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class TestGetGeneratedKeys {

    companion object {
        @RegisterExtension
        @JvmField
        val pg: EmbeddedPgExtension = MultiDatabaseBuilder.instanceWithDefaults().build()
    }

    @RegisterExtension
    @JvmField
    val pgExtension: JdbiExtension = JdbiExtension.postgres(pg)
        .withPlugins(SqlObjectPlugin(), KotlinSqlObjectPlugin())
        .withInitializer { _, h: Handle ->
            h.execute("create table something (id serial primary key, name varchar(50), integerValue integer, intValue integer)")
        }

    data class Something(val id: Long, val name: String, val intValue: Int, val integerValue: Int)

    interface StringDAO {

        @SqlUpdate("insert into something (name) values (:name)")
        @GetGeneratedKeys
        fun insert(@Bind("name") name: String): Long

        @SqlQuery("select name from something where id = :id")
        fun findNameById(@Bind("id") id: Long): String
    }

    interface StringSomethingDAO {
        @SqlUpdate("insert into something (name) values (:name) returning *")
        @GetGeneratedKeys
        @RegisterKotlinMapper(Something::class)
        fun insert(@Bind("name") name: String): Something

        @SqlQuery("select name from something where id = :id")
        fun findNameById(@Bind("id") id: Long): String
    }

    interface SomethingDAO {
        @SqlUpdate("insert into something (name, intValue, integerValue) values (:bean.name, :bean.intValue, :bean.integerValue) returning *")
        @GetGeneratedKeys
        @RegisterKotlinMapper(Something::class)
        fun insert(@BindBean("bean") bean: Something): Something

        @SqlQuery("select name from something where id = :id")
        fun findNameById(@Bind("id") id: Long): String
    }

    @Test
    fun testInsertStringReturnString() {
        pgExtension.jdbi.useExtension<StringDAO, RuntimeException>(StringDAO::class) { dao ->
            val brianId = dao.insert("Brian")
            val keithId = dao.insert("Keith")

            assertThat(dao.findNameById(brianId)).isEqualTo("Brian")
            assertThat(dao.findNameById(keithId)).isEqualTo("Keith")
        }
    }

    @Test
    fun testInsertStringReturnSomething() {
        pgExtension.jdbi.useExtension<StringSomethingDAO, RuntimeException>(StringSomethingDAO::class) { dao ->
            val brianRow = dao.insert("Brian")
            val keithRow = dao.insert("Keith")

            assertThat(dao.findNameById(brianRow.id)).isEqualTo("Brian")
            assertThat(dao.findNameById(keithRow.id)).isEqualTo("Keith")
        }
    }

    @Test
    fun testInsertSomethingReturnSomething() {
        pgExtension.jdbi.useExtension<SomethingDAO, RuntimeException>(SomethingDAO::class) { dao ->
            val brian = Something(id = 0, name = "Brian", intValue = 10, integerValue = 100)
            val keith = Something(id = 0, name = "Keith", intValue = 10, integerValue = 100)
            val brianRow = dao.insert(brian)
            val keithRow = dao.insert(keith)

            assertThat(dao.findNameById(brianRow.id)).isEqualTo("Brian")
            assertThat(dao.findNameById(keithRow.id)).isEqualTo("Keith")
        }
    }
}
