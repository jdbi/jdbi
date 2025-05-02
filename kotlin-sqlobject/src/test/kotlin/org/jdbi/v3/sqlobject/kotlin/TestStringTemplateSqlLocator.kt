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

import org.assertj.core.api.Assertions
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Something
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.customizer.Define
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.jdbi.v3.testing.junit5.internal.TestingInitializers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.sql.ResultSet
import java.sql.SQLException

// port from the stringtemplate4 class to kotlin
class TestStringTemplateSqlLocator {
    @RegisterExtension
    var h2Extension: JdbiExtension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(SqlObjectPlugin())

    private var handle: Handle? = null

    @BeforeEach
    fun setUp() {
        handle = h2Extension.getSharedHandle()
    }

    @Test
    fun testBaz() {
        val wombat =
            handle!!.attach(Wombat::class)
        wombat.insert(Something(7, "Henning"))

        val name = handle!!.createQuery("select name from something where id = 7")
            .mapTo(String::class)
            .one()

        Assertions.assertThat(name).isEqualTo("Henning")
    }

    @Test
    fun testBam() {
        handle!!.execute("insert into something (id, name) values (6, 'Martin')")

        val s = handle!!.attach(Wombat::class).findById(6L)
        Assertions.assertThat(s.getName()).isEqualTo("Martin")
    }

    @Test
    fun testBap() {
        handle!!.execute("insert into something (id, name) values (2, 'Bean')")
        val w = handle!!.attach(Wombat::class)
        Assertions.assertThat(w.findNameFor(2)).isEqualTo("Bean")
    }

    @Test
    fun testDefines() {
        handle!!.attach(Wombat::class)
            .weirdInsert("something", "id", "name", 5, "Bouncer")
        handle!!.attach(Wombat::class)
            .weirdInsert("something", "id", "name", 6, "Bean")
        val name = handle!!.createQuery("select name from something where id = 5")
            .mapTo(String::class.java)
            .one()

        Assertions.assertThat(name).isEqualTo("Bouncer")
    }

    @Test
    fun testConditionalExecutionWithNullValue() {
        handle!!.attach(Wombat::class)
            .insert(Something(6, "Jack"))
        handle!!.attach(Wombat::class.java)
            .insert(Something(7, "Wolf"))

        val somethings =
            handle!!.attach(Wombat::class).findByIdOrUptoLimit(6, null)
        Assertions.assertThat(somethings).hasSize(1)
    }

    @Test
    fun testConditionalExecutionWithNonNullValue() {
        handle!!.attach(Wombat::class)
            .insert(Something(6, "Jack"))
        handle!!.attach(Wombat::class).insert(Something(7, "Wolf"))

        val somethings = handle!!.attach(Wombat::class).findByIdOrUptoLimit(null, 8)
        Assertions.assertThat(somethings).hasSize(2)
    }

    @Test
    fun testBatching() {
        val roo =
            handle!!.attach(Wombat::class)
        roo.insertBunches(Something(1, "Jeff"), Something(2, "Brian"))

        Assertions.assertThat(roo.findById(1L)).isEqualTo(Something(1, "Jeff"))
        Assertions.assertThat(roo.findById(2L)).isEqualTo(Something(2, "Brian"))
    }

    //
    // Test a value class as a parameter for a sqlobject method - see https://github.com/jdbi/jdbi/issues/2790

    @JvmInline
    value class WeirdId(val value: Int)

    @Test
    fun testWeirdBap() {
        handle!!.execute("insert into something (id, name) values (2, 'Bean')")
        val w = handle!!.attach(Wombat::class)
        Assertions.assertThat(w.findNameForWeirdId(WeirdId(2))).isEqualTo("Bean")
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

        // A value class as a parameter requires an explicit value for the SQL query; Kotlin mangles the method name (findNameForWeirdId-<xxxx>) otherwise.
        @SqlQuery("findNameForWeirdId")
        fun findNameForWeirdId(@Bind("id") id: WeirdId): String?
    }

    class SomethingMapper : RowMapper<Something?> {
        @Throws(SQLException::class)
        override fun map(r: ResultSet, ctx: StatementContext?): Something = Something(r.getInt("id"), r.getString("name"))
    }
}
