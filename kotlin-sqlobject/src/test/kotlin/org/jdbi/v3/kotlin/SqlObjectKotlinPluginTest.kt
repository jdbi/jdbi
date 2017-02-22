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
package org.jdbi.v3.kotlin

import org.jdbi.v3.core.rule.H2DatabaseRule
import org.jdbi.v3.sqlobject.kotlin.attach
import org.jdbi.v3.sqlobject.kotlin.onDemand
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class SqlObjectKotlinPluginTest {
    @Rule @JvmField
    val db = H2DatabaseRule().withPlugins()

    data class Thing(val id: Int, val name: String,
                     val nullable: String?,
                     val nullableDefaultedNull: String? = null,
                     val nullableDefaultedNotNull: String? = "not null",
                     val defaulted: String = "default value")

    interface ThingDao {
        @SqlUpdate("insert into something (id, name) values (:something.id, :something.name)")
        fun insert(something: Thing)

        @SqlQuery("select id, name from something")
        fun list(): List<Thing>

        @SqlQuery("select id, name, null as nullable, null as nullableDefaultedNull, null as nullableDefaultedNotNull, 'test' as defaulted from something")
        fun listWithNulls(): List<Thing>

        @SqlQuery("select id, name from something where id=:id")
        fun findById(id: Int): Thing

        fun insertAndFind(something: Thing): Thing {
            insert(something)
            return findById(something.id)
        }
    }

    private fun commonTest(dao: ThingDao) {
        val brian = Thing(1, "Brian", null)
        val keith = Thing(2, "Keith", null)

        dao.insert(brian)
        dao.insert(keith)

        val rs = dao.list()

        assertEquals(2, rs.size.toLong())
        assertEquals(brian, rs[0])
        assertEquals(keith, rs[1])

        val foundThing = dao.findById(2)
        assertEquals(keith, foundThing)

        val rs2 = dao.listWithNulls()
        assertEquals(2, rs2.size.toLong())
        assertEquals(brian.copy(nullable = null, nullableDefaultedNull = null, nullableDefaultedNotNull = null, defaulted = "test"), rs2[0])
        assertEquals(keith.copy(nullable = null, nullableDefaultedNull = null, nullableDefaultedNotNull = null, defaulted = "test"), rs2[1])
    }

    @Test
    fun testDaoCanAttachViaDbiOnDemand() {
        commonTest(db.jdbi.onDemand(ThingDao::class))
    }

    @Test
    fun testDaoCanAttachViaHandleAttach() {
        commonTest(db.sharedHandle.attach(ThingDao::class))
    }

    @Test
    fun testDefaultMethod() {
        val dao = db.jdbi.onDemand(ThingDao::class)
        val brian = Thing(1, "Brian", null)

        val found = dao.insertAndFind(brian)
        assertEquals(brian, found)

    }




}