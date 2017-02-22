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

import org.jdbi.v3.core.rule.H2DatabaseRule
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class KotlinPluginTest {
    @Rule @JvmField
    val db = H2DatabaseRule().withPlugins()

    data class Thing(val id: Int, val name: String,
                     val nullable: String?,
                     val nullableDefaultedNull: String? = null,
                     val nullableDefaultedNotNull: String? = "not null",
                     val defaulted: String = "default value")


    @Test
    fun testFluentQuery() {
        val brian = Thing(1, "Brian", null)
        val keith = Thing(2, "Keith", null)

        val upd = db.sharedHandle.prepareBatch("insert into something (id, name) values (:id, :name)")
        listOf(brian, keith).forEach {
            upd.bindBean(it).add()
        }
        upd.execute()

        val qry = db.sharedHandle.createQuery("select id, name from something where id = :id")
        val things: List<Thing> = qry.bind("id", brian.id).map(Thing::class).list()
        assertEquals(1, things.size)
        assertEquals(brian, things[0])

        val qryAll = db.sharedHandle.createQuery("select id, name from something")
        qryAll.map(Thing::class).useSequence {
            assertEquals(keith, it.drop(1).first())
        }

        val qryAll2 = db.sharedHandle.createQuery("select id, name from something")
        qryAll2.useSequence(Thing::class) {
            assertEquals(keith, it.drop(1).first())
        }
    }


}