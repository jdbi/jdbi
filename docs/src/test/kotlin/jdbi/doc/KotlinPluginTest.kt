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

import org.jdbi.core.kotlin.mapTo
import org.jdbi.core.kotlin.useSequence
import org.jdbi.core.mapper.Nested
import org.jdbi.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.jdbi.sqlobject.kotlin.onDemand
import org.jdbi.sqlobject.statement.SqlQuery
import org.jdbi.sqlobject.statement.SqlUpdate
import org.jdbi.testing.junit.JdbiExtension
import org.jdbi.testing.junit.internal.TestingInitializers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertEquals

class KotlinPluginTest {
    @RegisterExtension
    @JvmField
    val h2Extension: JdbiExtension = JdbiExtension.h2()
        .withInitializer(TestingInitializers.something())
        .withPlugin(KotlinSqlObjectPlugin())

    // tag::dataClass[]
    data class IdAndName(val id: Int, val name: String)
    data class Thing(
        @Nested val idAndName: IdAndName,
        val nullable: String?,
        val nullableDefaultedNull: String? = null,
        val nullableDefaultedNotNull: String? = "not null",
        val defaulted: String = "default value"
    )
    // end::dataClass[]

    // tag::sqlObject[]
    interface ThingDao {
        @SqlUpdate("insert into something (id, name) values (:something.idAndName.id, :something.idAndName.name)")
        fun insert(something: Thing)

        @SqlQuery("select id, name from something")
        fun list(): List<Thing>
    }
    // end::sqlObject[]

    val brian = Thing(IdAndName(1, "Brian"), null)
    val keith = Thing(IdAndName(2, "Keith"), null)

    // tag::setUp[]
    @BeforeEach
    fun setUp() {
        val dao = h2Extension.jdbi.onDemand<ThingDao>()

        val brian = Thing(IdAndName(1, "Brian"), null)
        val keith = Thing(IdAndName(2, "Keith"), null)

        dao.insert(brian)
        dao.insert(keith)
    }
    // end::setUp[]

    // tag::testQuery[]
    @Test
    fun testFindById() {
        val qry = h2Extension.sharedHandle.createQuery("select id, name from something where id = :id")
        val things: List<Thing> = qry.bind("id", brian.idAndName.id).mapTo<Thing>().list()
        assertEquals(1, things.size)
        assertEquals(brian, things[0])
    }
    // end::testQuery[]

    @Test
    fun testFindAll() {
        val qryAll = h2Extension.sharedHandle.createQuery("select id, name from something")
        qryAll.mapTo<Thing>().useSequence {
            assertEquals(keith, it.drop(1).first())
        }
    }

    // tag::testDao[]
    @Test
    fun testDao() {
        val dao = h2Extension.jdbi.onDemand<ThingDao>()

        val rs = dao.list()

        assertEquals(2, rs.size.toLong())
        assertEquals(brian, rs[0])
        assertEquals(keith, rs[1])
    }
    // end::testDao[]
}
