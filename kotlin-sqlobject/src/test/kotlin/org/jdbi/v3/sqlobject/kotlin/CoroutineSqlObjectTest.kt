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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.kotlin.withExtensionUnchecked
import org.jdbi.v3.sqlobject.SqlObject
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.jdbi.v3.testing.junit5.internal.TestingInitializers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class CoroutineSqlObjectTest {

    @RegisterExtension
    @JvmField
    val h2Extension: JdbiExtension = JdbiExtension.h2()
        .withPlugin(KotlinPlugin(enableCoroutineSupport = true))
        .withPlugin(SqlObjectPlugin())
        .withInitializer(TestingInitializers.something())

    private lateinit var jdbi: Jdbi

    @BeforeEach
    fun setup() {
        jdbi = h2Extension.jdbi
    }

    data class Something(
        val id: Int,
        val name: String
    )

    @RegisterKotlinMapper(Something::class)
    interface SomethingDao : SqlObject {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        fun insert(@BindKotlin something: Something)

        @SqlQuery("select id, name from something where id=:id")
        fun findById(@Bind("id") id: Int): Something

        @SqlQuery("select id, name from something")
        fun list(): List<Something>
    }

    @Test
    fun testTransactionIODispatcher() {
        jdbi.withExtensionUnchecked(SomethingDao::class) { dao ->
            val origHandle = dao.handle

            val handle1: Handle?
            val handle2: Handle?
            val handle3: Handle?

            runBlocking(Dispatchers.IO) {
                coroutineScope {
                    // all Jdbi operations will map onto the attached handle in this coroutine context
                    handle1 = insertSomething(dao)

                    handle2 = selectSomething(dao, 1, "first name")
                    delay(1000L)
                }

                handle3 = selectSomething(dao, 2, "second name")
            }

            assertThat(origHandle).isSameAs(handle1).isSameAs(handle2).isSameAs(handle3)
        }
    }

    @Test
    fun testAsyncCoroutines() {
        jdbi.withExtensionUnchecked(SomethingDao::class) { dao ->
            val origHandle = dao.handle

            dao.insert(Something(1, "first name"))
            dao.insert(Something(2, "second name"))

            runBlocking {
                // run on separate threads, do not share handle
                val first = async { selectSomething(dao, 1, "first name") }
                val second = async {
                    delay(1000L)
                    selectSomething(dao, 2, "second name")
                }

                val firstHandle = first.await()
                val secondHandle = second.await()

                // and the threads do not share the handle, so they need to be different as well
                assertThat(origHandle).isSameAs(firstHandle).isSameAs(secondHandle)
            }
        }
    }

    private suspend fun insertSomething(dao: SomethingDao) = coroutineScope {
        dao.insert(Something(1, "first name"))
        dao.insert(Something(2, "second name"))
        dao.handle
    }

    private suspend fun selectSomething(dao: SomethingDao, id: Int, expected: String): Handle = coroutineScope {
        val result = dao.findById(id)
        assertThat(result.name).isEqualTo(expected)
        dao.handle
    }
}
