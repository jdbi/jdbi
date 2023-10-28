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

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.jdbi.v3.testing.junit5.internal.TestingInitializers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class CoroutineContextTest {

    @RegisterExtension
    @JvmField
    val h2Extension: JdbiExtension = JdbiExtension.h2().withPlugin(KotlinPlugin(enableCoroutineSupport = true))
        .withInitializer(TestingInitializers.something())

    private lateinit var jdbi: Jdbi

    @BeforeEach
    fun setup() {
        jdbi = h2Extension.jdbi
    }

    @Test
    fun testTransactionIODispatcher() {
        jdbi.inTransactionUnchecked { transactionHandle ->
            // empty attach clears out a possible handle in the current thread
            runBlocking(Dispatchers.IO + jdbi.inCoroutineContext()) {
                // empty attachToContext cleaned out the thread local
                assertThat(coroutineContext[CoroutineHandleScope.Key]?.handle).isNull()
                withContext(jdbi.inCoroutineContext(transactionHandle)) {
                    assertThat(coroutineContext[CoroutineHandleScope.Key]?.handle).isSameAs(transactionHandle)

                    // all Jdbi operations will map onto the attached handle in this coroutine context
                    val handle1 = insertSomething()
                    assertThat(handle1).isSameAs(transactionHandle)

                    val handle2 = countSomething(1, "Could not find result in transaction")
                    assertThat(handle2).isSameAs(transactionHandle)

                    delay(1000L)
                    transactionHandle.rollback()
                }

                val handle3 = countSomething(0, "Could not find result outside transaction")
                assertThat(handle3).isNotSameAs(transactionHandle)
            }
        }
    }

    @Test
    fun testTransactionDefaultDispatcher() {
        jdbi.inTransactionUnchecked { transactionHandle ->
            // empty attach clears out a possible handle in the current thread
            runBlocking(jdbi.inCoroutineContext()) {
                // empty attachToContext cleaned out the thread local
                assertThat(coroutineContext[CoroutineHandleScope.Key]?.handle).isNull()

                withContext(Dispatchers.Default + jdbi.inCoroutineContext(transactionHandle)) {
                    // all Jdbi operations will map onto the attached handle in this coroutine context
                    assertThat(coroutineContext[CoroutineHandleScope.Key]?.handle).isSameAs(transactionHandle)

                    val handle1 = insertSomething()
                    assertThat(handle1).isSameAs(transactionHandle)

                    val handle2 = countSomething(1, "Could not find result in transaction")
                    assertThat(handle2).isSameAs(transactionHandle)

                    delay(1000L)
                    transactionHandle.rollback()
                }

                // no handle attached
                val handle3 = countSomething(0, "Could not find result outside transaction")
                assertThat(handle3).isNotSameAs(transactionHandle)
            }
        }
    }

    private suspend fun insertSomething() = coroutineScope {
        jdbi.withHandleUnchecked { h ->
            h.execute("INSERT INTO something(id, name) VALUES(1, 'first name')")
            h
        }
    }

    private suspend fun countSomething(expected: Int, msg: String): Handle = coroutineScope {
        jdbi.withHandleUnchecked { h ->
            val count = h.createQuery("SELECT COUNT(*) as count FROM something").mapTo(Int::class).single()
            assertThat(count).withFailMessage(msg).isEqualTo(expected)
            h
        }
    }

    @Test
    fun testAsyncCoroutines() {
        with(h2Extension.sharedHandle) {
            execute("INSERT INTO something(id, name) VALUES(1, 'first name')")
            execute("INSERT INTO something(id, name) VALUES(2, 'second name')")
        }

        runBlocking(jdbi.inCoroutineContext()) {
            assertThat(coroutineContext[CoroutineHandleScope.Key]?.handle).isNull()

            // run on separate threads, do not share handle
            val first = async { selectSomething(1, "first name") }
            val second = async {
                delay(1000L)
                selectSomething(2, "second name")
            }

            val firstHandle = first.await()
            val secondHandle = second.await()

            // and the threads do not share the handle, so they need to be different as well
            assertThat(firstHandle).isNotSameAs(secondHandle)
        }
    }

    @Test
    @OptIn(DelicateCoroutinesApi::class)
    fun testSingleThreadedCoroutines() {
        with(h2Extension.sharedHandle) {
            execute("INSERT INTO something(id, name) VALUES(1, 'first name')")
            execute("INSERT INTO something(id, name) VALUES(2, 'second name')")
        }

        newSingleThreadContext("test-dispatcher").use { dispatcher ->
            runBlocking(dispatcher + jdbi.inCoroutineContext()) {
                assertThat(coroutineContext[CoroutineHandleScope.Key]?.handle).isNull()

                // run on the same thread, but do not share handle
                val handle1 = selectSomething(1, "first name")
                val handle2 = selectSomething(2, "second name")

                // but they do not share the handle
                assertThat(handle1).isNotSameAs(handle2)
            }
        }
    }

    @Test
    fun testCoroutinesWithSharedHandle() {
        with(h2Extension.sharedHandle) {
            execute("INSERT INTO something(id, name) VALUES(1, 'first name')")
            execute("INSERT INTO something(id, name) VALUES(2, 'second name')")
        }

        runBlocking(Dispatchers.IO + jdbi.inCoroutineContext(h2Extension.sharedHandle)) {
            assertThat(coroutineContext[CoroutineHandleScope.Key]?.handle).isSameAs(h2Extension.sharedHandle)

            val handle1 = selectSomething(1, "first name")
            val handle2 = selectSomething(2, "second name")

            assertThat(handle1).isSameAs(handle2)
            assertThat(handle1).isSameAs(h2Extension.sharedHandle)
        }
    }

    @Test
    fun testAsyncCoroutinesWithSharedHandle() {
        with(h2Extension.sharedHandle) {
            execute("INSERT INTO something(id, name) VALUES(1, 'first name')")
            execute("INSERT INTO something(id, name) VALUES(2, 'second name')")
        }

        runBlocking(jdbi.inCoroutineContext(h2Extension.sharedHandle)) {
            assertThat(coroutineContext[CoroutineHandleScope.Key]?.handle).isSameAs(h2Extension.sharedHandle)

            // run on separate threads, do not share handle
            val first = async { selectSomething(1, "first name") }
            val second = async {
                delay(1000L)
                selectSomething(2, "second name")
            }

            val firstHandle = first.await()
            val secondHandle = second.await()

            assertThat(firstHandle).isSameAs(secondHandle)
            assertThat(firstHandle).isSameAs(h2Extension.sharedHandle)
        }
    }

    @Test
    fun testFlow() {
        with(h2Extension.sharedHandle) {
            execute("INSERT INTO something(id, name) VALUES(1, 'first name')")
            execute("INSERT INTO something(id, name) VALUES(2, 'second name')")
            execute("INSERT INTO something(id, name) VALUES(3, 'third name')")
        }

        runBlocking(Dispatchers.IO + jdbi.inCoroutineContext(h2Extension.sharedHandle)) {
            assertThat(coroutineContext[CoroutineHandleScope.Key]?.handle).isSameAs(h2Extension.sharedHandle)

            // collect all results from a flow asynchronously and ensure that
            // all operations use the same handle
            fetchFlow().collect { value -> assertThat(value).isSameAs(h2Extension.sharedHandle) }
        }
    }

    private fun fetchFlow(): Flow<Handle> = flow {
        emit(selectSomething(1, "first name"))
        emit(selectSomething(2, "second name"))
        emit(selectSomething(3, "third name"))
    }

    private suspend fun selectSomething(id: Int, expected: String): Handle = coroutineScope {
        jdbi.withHandleUnchecked { h ->
            val name = h.createQuery("SELECT name FROM something WHERE id = :id")
                .bind("id", id)
                .mapTo(String::class)
                .single()
            assertThat(name).isEqualTo(expected)
            h
        }
    }
}
