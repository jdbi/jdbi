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

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.core.kotlin.KotlinMapper
import org.jdbi.sqlobject.customizer.Bind
import org.jdbi.sqlobject.statement.SqlQuery
import org.jdbi.testing.junit5.JdbiExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class TestIssue1577 {

    companion object {
        @RegisterExtension
        val pg: EmbeddedPgExtension = MultiDatabaseBuilder.instanceWithDefaults().build()
    }

    private lateinit var onDemandDao: DataDao

    @RegisterExtension
    @JvmField
    val pgExtension: JdbiExtension = JdbiExtension.postgres(pg)
        .withInitializer { ds, handle ->
            handle.execute("CREATE TABLE data (id serial primary key, comma varchar(50))")
            handle.execute("INSERT INTO data (comma) VALUES ('one,two,three,four')")
        }
        .withPlugin(KotlinSqlObjectPlugin())

    @BeforeEach
    fun setup() {
        val jdbi = pgExtension.jdbi
        jdbi.registerRowMapper(KotlinMapper(Data::class))

        this.onDemandDao = jdbi.onDemand(DataDao::class.java)
    }

    @Test
    fun testOnDemand() {
        val data = onDemandDao.getData(1)
        assertThat(data.comma).isNotEmpty()
    }

    @Test
    fun testAttach() {
        val handle = pgExtension.sharedHandle
        val localDao = handle.attach(DataDao::class)
        val data = localDao.getData(1)
        assertThat(data.comma).isNotEmpty()
    }

    @Test
    fun testExtension() {
        val data = pgExtension.jdbi.withExtension<Data, DataDao, RuntimeException>(DataDao::class.java, { dao -> dao.getData(1) })
        assertThat(data.comma).isNotEmpty()
    }

    data class Data(val comma: String)

    interface BaseDao<T> {
        @SqlQuery
        fun getData(id: Int): T
    }

    interface DataDao : BaseDao<Data> {
        @SqlQuery("SELECT comma FROM data where id = :id")
        override fun getData(@Bind("id") id: Int): Data
    }
}
