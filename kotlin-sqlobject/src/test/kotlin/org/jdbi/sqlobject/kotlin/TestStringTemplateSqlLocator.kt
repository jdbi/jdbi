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
import org.jdbi.core.Something
import org.jdbi.core.kotlin.mapTo
import org.jdbi.core.mapper.RowMapper
import org.jdbi.core.statement.StatementContext
import org.jdbi.sqlobject.config.RegisterRowMapper
import org.jdbi.sqlobject.customizer.Bind
import org.jdbi.sqlobject.customizer.BindBean
import org.jdbi.sqlobject.customizer.Define
import org.jdbi.sqlobject.statement.SqlBatch
import org.jdbi.sqlobject.statement.SqlQuery
import org.jdbi.sqlobject.statement.SqlUpdate
import org.jdbi.stringtemplate4.UseStringTemplateSqlLocator
import org.jdbi.testing.junit5.JdbiExtension
import org.jdbi.testing.junit5.internal.TestingInitializers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.sql.ResultSet
import java.sql.SQLException

// port from the stringtemplate4 class to kotlin
class TestStringTemplateSqlLocator {
    @RegisterExtension
    var h2Extension: JdbiExtension = JdbiExtension.h2()
        .withPlugin(KotlinSqlObjectPlugin())
        .withInitializer(TestingInitializers.something())

    private lateinit var handle: Handle
    private lateinit var wombat: Wombat

    @BeforeEach
    fun setUp() {
        handle = h2Extension.getSharedHandle()
        wombat = handle.attach(Wombat::class)
    }

    @Test
    fun testBaz() {
        wombat.insert(Something(7, "Henning"))

        val name = handle.createQuery("select name from something where id = 7")
            .mapTo(String::class)
            .one()

        assertThat(name).isEqualTo("Henning")
    }

    @Test
    fun testBam() {
        handle.execute("insert into something (id, name) values (6, 'Martin')")

        val s = wombat.findById(6L)
        assertThat(s.name).isEqualTo("Martin")
    }

    @Test
    fun testBap() {
        handle.execute("insert into something (id, name) values (2, 'Bean')")
        assertThat(wombat.findNameFor(2)).isEqualTo("Bean")
    }

    @Test
    fun testDefines() {
        wombat.weirdInsert("something", "id", "name", 5, "Bouncer")
        wombat.weirdInsert("something", "id", "name", 6, "Bean")
        val name = handle.createQuery("select name from something where id = 5")
            .mapTo(String::class.java)
            .one()

        assertThat(name).isEqualTo("Bouncer")
    }

    @Test
    fun testConditionalExecutionWithNullValue() {
        wombat.insert(Something(6, "Jack"))
        wombat.insert(Something(7, "Wolf"))

        val somethings = wombat.findByIdOrUptoLimit(6, null)
        assertThat(somethings).hasSize(1)
    }

    @Test
    fun testConditionalExecutionWithNonNullValue() {
        wombat.insert(Something(6, "Jack"))
        wombat.insert(Something(7, "Wolf"))

        val somethings = wombat.findByIdOrUptoLimit(null, 8)
        assertThat(somethings).hasSize(2)
    }

    @Test
    fun testBatching() {
        wombat.insertBunches(Something(1, "Jeff"), Something(2, "Brian"))

        assertThat(wombat.findById(1L)).isEqualTo(Something(1, "Jeff"))
        assertThat(wombat.findById(2L)).isEqualTo(Something(2, "Brian"))
    }

    @UseStringTemplateSqlLocator
    @RegisterRowMapper(SomethingMapper::class)
    interface Wombat {
        @SqlUpdate
        fun insert(@BindBean s: Something?)

        @SqlQuery
        fun findById(@Bind("id") id: Long?): Something

        @SqlQuery
        fun findByIdOrUptoLimit(@Bind("id") id: Int?, @Define("idLimit") @Bind("idLimit") idLimit: Int?): MutableList<Something?>?

        @SqlQuery
        fun findNameFor(@Bind("id") id: Int): String?

        @SqlUpdate
        fun weirdInsert(
            @Define("table") table: String?,
            @Define("id_column") idColumn: String?,
            @Define("value_column") valueColumn: String?,
            @Bind("id") id: Int,
            @Bind("value") name: String?
        )

        @SqlBatch
        fun insertBunches(@BindBean vararg somethings: Something?)
    }

    class SomethingMapper : RowMapper<Something?> {
        @Throws(SQLException::class)
        override fun map(r: ResultSet, ctx: StatementContext?): Something = Something(r.getInt("id"), r.getString("name"))
    }
}
