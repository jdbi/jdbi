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

import org.jdbi.sqlobject.SqlObject
import org.jdbi.sqlobject.statement.SqlBatch
import org.jdbi.sqlobject.statement.SqlQuery
import org.jdbi.testing.junit5.JdbiExtension
import org.jdbi.testing.junit5.internal.TestingInitializers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class TestBatch {
    @RegisterExtension
    @JvmField
    val h2Extension: JdbiExtension = JdbiExtension.h2()
        .withInitializer(TestingInitializers.something())
        .withPlugin(KotlinSqlObjectPlugin())

    data class Something(val id: Int, val name: String)

    @JvmInline
    value class SomethingId(val id: Int)

    data class ValueSomething(val id: SomethingId, val name: String)

    interface SomethingDao : SqlObject {
        @SqlBatch("insert into something (id, name) values (:something.id, :something.name)")
        fun insert(something: List<Something>)

        @SqlBatch("insert into something (id, name) values (:something.id, :something.name)")
        fun insertValue(something: List<ValueSomething>)

        @SqlBatch("insert into something (id, name) values (:something.id, :something.name)")
        fun insertJava(something: java.util.List<Something>)

        @SqlBatch("insert into something (id, name) values (:something.id, :something.name)")
        fun insertJavaValue(something: java.util.List<ValueSomething>)

        @SqlQuery("select id, name from something")
        fun list(): List<Something>

        @SqlQuery("select id, name from something")
        fun listValue(): List<ValueSomething>

        @SqlQuery("select id, name from something")
        fun listJava(): java.util.List<Something>

        @SqlQuery("select id, name from something")
        fun listJavaValue(): java.util.List<ValueSomething>
    }

    @Test
    fun testSimpleBatch() {
        val dao = h2Extension.jdbi.onDemand<SomethingDao>()

        val list = listOf(Something(1, "one"), Something(2, "two"))
        dao.insert(list)

        val result = dao.list()
        assertEquals(list.size, result.size)
        assertEquals(list[0], result[0])
        assertEquals(list[1], result[1])
    }

    @Test
    fun testValueBatch() {
        val dao = h2Extension.jdbi.onDemand<SomethingDao>()

        val list = listOf(ValueSomething(SomethingId(1), "one"), ValueSomething(SomethingId(2), "two"))
        dao.insertValue(list)

        val result = dao.listValue()
        assertEquals(list.size, result.size)
        assertEquals(list[0], result[0])
        assertEquals(list[1], result[1])
    }

    @Test
    fun testJavaBatch() {
        val dao = h2Extension.jdbi.onDemand<SomethingDao>()

        val list: java.util.List<Something> = java.util.List.of(
            Something(1, "one"),
            Something(2, "two")
        ) as java.util.List<Something>

        dao.insertJava(list)

        val result: java.util.List<Something> = dao.listJava()
        assertEquals(list.size, result.size)
        assertEquals(list[0], result[0])
        assertEquals(list[1], result[1])
    }

    @Test
    fun testJavaBatchValue() {
        val dao = h2Extension.jdbi.onDemand<SomethingDao>()

        val list: java.util.List<ValueSomething> = java.util.List.of(
            ValueSomething(SomethingId(1), "one"),
            ValueSomething(SomethingId(2), "two")
        ) as java.util.List<ValueSomething>

        dao.insertJavaValue(list)

        val result: java.util.List<ValueSomething> = dao.listJavaValue()
        assertEquals(list.size, result.size)
        assertEquals(list[0], result[0])
        assertEquals(list[1], result[1])
    }
}
