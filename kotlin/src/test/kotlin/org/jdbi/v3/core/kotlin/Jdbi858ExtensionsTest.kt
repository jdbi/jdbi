/**
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

import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.rule.H2DatabaseRule
import org.jdbi.v3.core.transaction.TransactionIsolationLevel
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaMethod

class Jdbi858ExtensionsTest {
    interface FooDao {
        @SqlQuery("SELECT ${Jdbi858ExtensionsTest.NAME_COLUMN} FROM ${Jdbi858ExtensionsTest.TABLE_NAME}")
        fun getOnlyName(): String
    }

    @Rule @JvmField
    val dbRule = H2DatabaseRule().withPlugins()

    lateinit var jdbi: Jdbi

    @Before fun setUp() {
        jdbi = dbRule.jdbi

        dbRule.openHandle().apply {
            execute("CREATE TABLE $TABLE_NAME ($ID_COLUMN INTEGER PRIMARY KEY AUTO_INCREMENT, $NAME_COLUMN VARCHAR)")
            execute("INSERT INTO $TABLE_NAME ($NAME_COLUMN) VALUES (?)", EXPECTED_NAME)
        }
    }

    @Test fun testWithHandleUnchecked() {
        val name = jdbi.withHandleUnchecked { handle ->
            handle.createQuery("SELECT $NAME_COLUMN FROM $TABLE_NAME").mapTo(String::class.java).one()
        }

        assertThat(name).isEqualTo(EXPECTED_NAME)
    }

    @Test fun testUseHandleUnchecked() {
        jdbi.useHandleUnchecked { handle ->
            val name = handle.createQuery("SELECT $NAME_COLUMN FROM $TABLE_NAME").mapTo(String::class.java).one()

            assertThat(name).isEqualTo(EXPECTED_NAME)
        }
    }

    @Test fun testInTransactionUnchecked() {
        val name = jdbi.inTransactionUnchecked { handle ->
            handle.createQuery("SELECT $NAME_COLUMN FROM $TABLE_NAME").mapTo(String::class.java).one()
        }

        assertThat(name).isEqualTo(EXPECTED_NAME)
    }

    @Test fun testUseTransactionUnchecked() {
        jdbi.useTransactionUnchecked { handle ->
            val name = handle.createQuery("SELECT $NAME_COLUMN FROM $TABLE_NAME").mapTo(String::class.java).one()

            assertThat(name).isEqualTo(EXPECTED_NAME)
        }
    }

    @Test fun testInTransactionUncheckedWithLevel() {
        val name = jdbi.inTransactionUnchecked(TransactionIsolationLevel.READ_COMMITTED) { handle ->
            handle.createQuery("SELECT $NAME_COLUMN FROM $TABLE_NAME").mapTo(String::class.java).one()
        }

        assertThat(name).isEqualTo(EXPECTED_NAME)
    }

    @Test fun testUseTransactionUncheckedWithLevel() {
        jdbi.useTransactionUnchecked(TransactionIsolationLevel.READ_COMMITTED) { handle ->
            val name = handle.createQuery("SELECT $NAME_COLUMN FROM $TABLE_NAME").mapTo(String::class.java).one()

            assertThat(name).isEqualTo(EXPECTED_NAME)
        }
    }

    @Test fun testWithExtensionUnchecked() {
        val name = jdbi.withExtensionUnchecked(FooDao::class.java) { dao ->
            dao.getOnlyName()
        }

        assertThat(name).isEqualTo(EXPECTED_NAME)
    }

    @Test fun testUseExtensionUnchecked() {
        jdbi.useExtensionUnchecked(FooDao::class.java) { dao ->
            val name = dao.getOnlyName()

            assertThat(name).isEqualTo(EXPECTED_NAME)
        }
    }

    @Test fun testWithExtensionUncheckedKClass() {
        val name = jdbi.withExtensionUnchecked(FooDao::class) { dao ->
            dao.getOnlyName()
        }

        assertThat(name).isEqualTo(EXPECTED_NAME)
    }

    @Test fun testUseExtensionUncheckedKClass() {
        jdbi.useExtensionUnchecked(FooDao::class) { dao ->
            val name = dao.getOnlyName()

            assertThat(name).isEqualTo(EXPECTED_NAME)
        }
    }

    @Test fun javaAnnotationsEqualToKotlinAnnotations() {
        val kotlinAnnotation = FooDao::getOnlyName.findAnnotation<SqlQuery>()
        val javaAnnotation = FooDao::getOnlyName.javaMethod?.getAnnotation(SqlQuery::class.java)
        assertThat(kotlinAnnotation)
            .isNotNull()
            .isEqualTo(javaAnnotation)
    }

    companion object {
        const val EXPECTED_NAME = "Foo"
        const val TABLE_NAME = "FOO"
        const val ID_COLUMN = "id"
        const val NAME_COLUMN = "name"
    }
}
