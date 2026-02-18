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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.core.Handle
import org.jdbi.core.Jdbi
import org.jdbi.core.kotlin.withExtensionUnchecked
import org.jdbi.sqlobject.SqlObject
import org.jdbi.sqlobject.customizer.Bind
import org.jdbi.sqlobject.statement.SqlQuery
import org.jdbi.sqlobject.statement.SqlUpdate
import org.jdbi.testing.junit.JdbiExtension
import org.jdbi.testing.junit.internal.TestingInitializers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.lang.Boolean.TRUE
import java.util.concurrent.ArrayBlockingQueue

class CoroutineSqlObjectTest {

    @RegisterExtension
    @JvmField
    val h2Extension: JdbiExtension = JdbiExtension.h2()
        .withPlugin(KotlinSqlObjectPlugin(enableCoroutineSupport = true))
        .withInitializer(TestingInitializers.something())

    private lateinit var jdbi: Jdbi

    @BeforeEach
    fun setup() {
        jdbi = h2Extension.jdbi
    }

    data class Something(val id: Int, val name: String)

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
        val queue = ArrayBlockingQueue<Boolean>(1)

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
                    queue.poll()
                }

                handle3 = selectSomething(dao, 2, "second name")
                queue.add(TRUE)
            }

            assertThat(origHandle).isSameAs(handle1).isSameAs(handle2).isSameAs(handle3)
        }
    }

    @Test
    fun testAsyncCoroutines() {
        val queue = ArrayBlockingQueue<Boolean>(1)

        jdbi.withExtensionUnchecked(SomethingDao::class) { dao ->
            val origHandle = dao.handle

            dao.insert(Something(1, "first name"))
            dao.insert(Something(2, "second name"))

            runBlocking {
                // run on separate threads, do not share handle
                val first = async {
                    selectSomething(dao, 1, "first name").also {
                        queue.put(TRUE)
                    }
                }

                val second = async {
                    queue.poll()
                    selectSomething(dao, 2, "second name")
                }

                val firstHandle = first.await()
                val secondHandle = second.await()

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
