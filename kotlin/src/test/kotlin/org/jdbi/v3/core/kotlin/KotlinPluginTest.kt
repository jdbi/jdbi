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

import org.jdbi.v3.core.extension.ExtensionCallback
import org.jdbi.v3.core.extension.ExtensionConsumer
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.jdbi.v3.testing.junit5.internal.TestingInitializers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertEquals

class KotlinPluginTest {
    @RegisterExtension
    @JvmField
    val h2Extension: JdbiExtension = JdbiExtension.h2().withPlugins(KotlinPlugin(), SqlObjectPlugin()).withInitializer(TestingInitializers.something())

    data class Thing(
        val id: Int,
        val name: String,
        val nullable: String?,
        val nullableDefaultedNull: String? = null,
        val nullableDefaultedNotNull: String? = "not null",
        val defaulted: String = "default value"
    )

    interface ThingDao {
        @SqlQuery("select id, name from something where id = :id")
        fun getById(@Bind("id") id: Int): Thing
    }

    val brian = Thing(1, "Brian", null)
    val keith = Thing(2, "Keith", null)

    @BeforeEach
    fun setUp() {
        val upd = h2Extension.sharedHandle.prepareBatch("insert into something (id, name) values (:id, :name)")
        listOf(brian, keith).forEach {
            upd.bindBean(it).add()
        }
        upd.execute()
    }

    @Test
    fun testFindById() {
        val qry = h2Extension.sharedHandle.createQuery("select id, name from something where id = :id")
        val things: List<Thing> = qry.bind("id", brian.id).mapTo<Thing>().list()
        assertEquals(1, things.size)
        assertEquals(brian, things[0])
    }

    @Test
    fun testFindByIdWithNulls() {
        val qry = h2Extension.sharedHandle.createQuery(
            "select " +
                "id, " +
                "name, " +
                "null as nullable, " +
                "null as nullableDefaultedNull, " +
                "null as nullableDefaultedNotNull, " +
                "'test' as defaulted " +
                "from something where id = :id"
        )
        val things: List<Thing> = qry.bind("id", brian.id).mapTo<Thing>().list()
        assertEquals(1, things.size)
        assertEquals(brian.copy(nullableDefaultedNotNull = null, defaulted = "test"), things[0])
    }

    @Test
    fun testFindAll() {
        val qryAll = h2Extension.sharedHandle.createQuery("select id, name from something")
        qryAll.mapTo<Thing>().useSequence {
            assertEquals(keith, it.drop(1).first())
        }
    }

    @Test
    fun testWithExtensionKClass() {
        val result = h2Extension.jdbi.withExtension(
            ThingDao::class,
            ExtensionCallback<Thing, ThingDao, RuntimeException> { extension ->
                extension.getById(brian.id)
            }
        )

        assertEquals(result, brian)
    }

    @Test
    fun testUseExtensionKClass() {
        h2Extension.jdbi.useExtension(
            ThingDao::class,
            ExtensionConsumer<ThingDao, RuntimeException> { extension ->
                val result = extension.getById(brian.id)
                assertEquals(result, brian)
            }
        )
    }
}
