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
package jdbi.doc

import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.kotlin.useSequence
import org.jdbi.v3.core.rule.H2DatabaseRule
import org.jdbi.v3.sqlobject.kotlin.onDemand
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class KotlinPluginTest {
    @Rule @JvmField
    val db = H2DatabaseRule().withPlugins()

    // tag::dataClass[]
    data class Thing(val id: Int, val name: String,
                     val nullable: String?,
                     val nullableDefaultedNull: String? = null,
                     val nullableDefaultedNotNull: String? = "not null",
                     val defaulted: String = "default value")
    // end::dataClass[]

    // tag::sqlObject[]
    interface ThingDao {
        @SqlUpdate("insert into something (id, name) values (:something.id, :something.name)")
        fun insert(something: Thing)

        @SqlQuery("select id, name from something")
        fun list(): List<Thing>
    }
    // end::sqlObject[]


    val brian = Thing(1, "Brian", null)
    val keith = Thing(2, "Keith", null)

    // tag::setUp[]
    @Before fun setUp() {
        val dao = db.jdbi.onDemand<ThingDao>()

        val brian = Thing(1, "Brian", null)
        val keith = Thing(2, "Keith", null)

        dao.insert(brian)
        dao.insert(keith)
    }
    // end::setUp[]

    // tag::testQuery[]
    @Test fun testFindById() {
        val qry = db.sharedHandle.createQuery("select id, name from something where id = :id")
        val things: List<Thing> = qry.bind("id", brian.id).mapTo<Thing>().list()
        assertEquals(1, things.size)
        assertEquals(brian, things[0])
    }
    // end::testQuery[]


    @Test fun testFindAll() {

        val qryAll = db.sharedHandle.createQuery("select id, name from something")
        qryAll.mapTo<Thing>().useSequence {
            assertEquals(keith, it.drop(1).first())
        }

    }

    // tag::testDao[]
    @Test fun testDao() {
        val dao = db.jdbi.onDemand<ThingDao>()

        val rs = dao.list()

        assertEquals(2, rs.size.toLong())
        assertEquals(brian, rs[0])
        assertEquals(keith, rs[1])

    }
    // end::testDao[]

}