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

import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.reflect.ColumnName
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor
import org.jdbi.v3.core.qualifier.Reversed
import org.jdbi.v3.core.qualifier.ReversedStringArgumentFactory
import org.jdbi.v3.core.qualifier.ReversedStringMapper
import org.jdbi.v3.sqlobject.SqlObject
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper
import org.jdbi.v3.sqlobject.config.RegisterConstructorMappers
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.jdbi.v3.testing.junit5.internal.TestingInitializers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertEquals
import kotlin.test.assertFails

class KotlinSqlObjectPluginTest {
    @RegisterExtension
    @JvmField
    val h2Extension: JdbiExtension = JdbiExtension.h2().installPlugins().withInitializer(TestingInitializers.something())

    data class Thing(
        val id: Int,
        val name: String,
        val nullable: String?,
        val nullableDefaultedNull: String? = null,
        val nullableDefaultedNotNull: String? = "not null",
        val defaulted: String = "default value"
    )

    interface ThingDao : SqlObject {
        @SqlUpdate("insert into something (id, name) values (:something.id, :something.name)")
        fun insert(something: Thing)

        fun list(): List<Thing> {
            return handle.createQuery("select id, name from something")
                .mapTo(Thing::class.java)
                .list()
        }

        @SqlQuery("select id, name, null as nullable, null as nullableDefaultedNull, null as nullableDefaultedNotNull, 'test' as defaulted from something")
        fun listWithNulls(): List<Thing>

        @SqlQuery("select id, name from something where id=:id")
        fun findById(id: Int): Thing

        @SqlQuery("select id as t_id, name as t_name from something where id=:id")
        @RegisterKotlinMapper(Thing::class, "t")
        fun findByIdWithAlias(id: Int): Thing

        fun insertAndFind(something: Thing): Thing {
            insert(something)
            return findById(something.id)
        }

        fun throwsException(e: Exception): Thing {
            throw e
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

        val foundThingWithAlias = dao.findByIdWithAlias(2)
        assertEquals(keith, foundThingWithAlias)

        val rs2 = dao.listWithNulls()
        assertEquals(2, rs2.size.toLong())
        assertEquals(brian.copy(nullable = null, nullableDefaultedNull = null, nullableDefaultedNotNull = null, defaulted = "test"), rs2[0])
        assertEquals(keith.copy(nullable = null, nullableDefaultedNull = null, nullableDefaultedNotNull = null, defaulted = "test"), rs2[1])
    }

    @Test
    fun testDaoCanAttachViaDbiOnDemand() {
        commonTest(h2Extension.jdbi.onDemand<ThingDao>())
    }

    @Test
    fun testDaoCanAttachViaDbiOnDemandWithKClassArgument() {
        commonTest(h2Extension.jdbi.onDemand(ThingDao::class))
    }

    @Test
    fun testDaoCanAttachViaHandleAttach() {
        commonTest(h2Extension.sharedHandle.attach<ThingDao>())
    }

    @Test
    fun testDaoCanAttachViaHandleAttachWithKClassArgument() {
        commonTest(h2Extension.sharedHandle.attach(ThingDao::class))
    }

    @Test
    fun testDefaultMethod() {
        val dao = h2Extension.jdbi.onDemand<ThingDao>()
        val brian = Thing(1, "Brian", null)

        val found = dao.insertAndFind(brian)
        assertEquals(brian, found)
    }

    @Test
    fun testDefaultMethodShouldPropagateException() {
        val dao = h2Extension.jdbi.onDemand<ThingDao>()
        val exception = UnsupportedOperationException("Testing exception propagation")
        val actualException = assertFails { dao.throwsException(exception) }
        assertEquals(exception, actualException)
    }

    @Test
    fun qualifiedBindParameter() {
        val dao = h2Extension.jdbi.onDemand<QualifiedDao>()
        dao.insert(1, "abc")
        assertThat(
            h2Extension.sharedHandle
                .select("SELECT name FROM something WHERE id = 1")
                .mapTo<String>()
                .one()
        )
            .isEqualTo("cba")

        h2Extension.sharedHandle.execute("insert into something (id, name) values (2, 'xyz')")

        assertThat(dao.select(2)).isEqualTo("zyx")
    }

    @RegisterArgumentFactory(ReversedStringArgumentFactory::class)
    @RegisterColumnMapper(ReversedStringMapper::class)
    interface QualifiedDao {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        fun insert(id: Int, @Reversed name: String)

        @SqlQuery("select name from something where id = :id")
        @Reversed
        fun select(id: Int): String
    }

    data class DataClassWithJdbiConstructor @JdbiConstructor constructor(@ColumnName("not_s") val s: String, @ColumnName("i") val i: Int = 5)

    interface TestDao {
        @SqlQuery("SELECT s as not_s, i as i from bean where s = :s")
        @RegisterConstructorMappers(RegisterConstructorMapper(DataClassWithJdbiConstructor::class))
        fun findOne(s: String): DataClassWithJdbiConstructor
    }

    @Test
    fun testDataClassWithJdbiConstructor() {
        h2Extension.sharedHandle.execute("CREATE TABLE bean (s varchar, i integer)")

        h2Extension.sharedHandle.execute("INSERT INTO bean VALUES('x', 2)")

        val dao = h2Extension.jdbi.onDemand(TestDao::class.java)

        val result = dao.findOne("x")

        assertThat(result.s).isEqualTo("x")
        assertThat(result.i).isEqualTo(2)
    }

    @RegisterKotlinMappers(
        RegisterKotlinMapper(Thing::class, "t")
    )
    interface YetAnotherThingDao : SqlObject {
        @SqlUpdate("insert into something (id, name) values (:something.id, :something.name)")
        fun insert(something: Thing)

        @SqlQuery("select id as t_id, name as t_name from something where id=:id")
        fun findById(id: Int): Thing
    }

    @Test
    fun testRegisterMappersAtInterfaceLevel() {
        val dao = h2Extension.jdbi.onDemand(YetAnotherThingDao::class.java)

        dao.insert(Thing(42, "Douglas", null))
        val result = dao.findById(42)

        assertThat(result.id).isEqualTo(42)
        assertThat(result.name).isEqualTo("Douglas")
    }
}
