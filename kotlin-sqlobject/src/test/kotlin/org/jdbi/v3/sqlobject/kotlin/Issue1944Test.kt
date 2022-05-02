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

import org.jdbi.v3.core.HandleCallback
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinMapper
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.JoinRow
import org.jdbi.v3.core.mapper.JoinRowMapper
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class Issue1944Test {
    @JvmField
    @RegisterExtension
    var h2Extension: JdbiExtension = JdbiExtension.h2()
        .withPlugins(SqlObjectPlugin(), KotlinSqlObjectPlugin())
        .withInitializer { _, handle ->
            handle.inTransaction(
                HandleCallback<Any?, RuntimeException> { h ->
                    h.execute("CREATE TABLE Tag (id integer primary key, name VARCHAR(50))")
                    h.execute("CREATE TABLE Product (id integer primary key, primaryName varchar(50), tagId integer)")

                    h.execute("INSERT INTO Tag (id, name) VALUES (1, 'foo')")
                    h.execute("INSERT INTO Product (id, primaryName, tagId) VALUES (2, 'stuff', 1)")
                }
            )
        }

    data class Tag(
        val id: Int?,
        val name: String?
    )

    data class Product(
        val id: Int?,
        val primaryName: String?,
        val tagId: Int?
    )

    @Test
    fun testWithKotlinMapperFactory(jdbi: Jdbi) {
        jdbi.installPlugin(KotlinPlugin())
            // the required mapper for Tag is implicit and will be created on the fly without a prefix by the KotlinMapperFactory
            .registerRowMapper(KotlinMapper(Product::class.java, "p"))
            .registerRowMapper(JoinRowMapper.forTypes(Tag::class.java, Product::class.java))

        doTest(jdbi)
    }

    @Test
    fun testWithoutKotlinMapperFactory(jdbi: Jdbi) {
        jdbi.installPlugin(KotlinPlugin(false))
            .registerRowMapper(KotlinMapper(Product::class.java, "p"))
            .registerRowMapper(KotlinMapper(Tag::class.java))
            .registerRowMapper(JoinRowMapper.forTypes(Tag::class.java, Product::class.java))

        doTest(jdbi)
    }

    @Test
    fun testNativeKotlin(jdbi: Jdbi) {
        jdbi.installPlugin(KotlinPlugin(false))
            .registerRowMapper(KotlinMapper(Product::class, "p"))
            .registerRowMapper(KotlinMapper(Tag::class))
            .registerRowMapper(JoinRowMapper.forTypes(Tag::class.java, Product::class.java))

        doTest(jdbi)
    }

    fun doTest(jdbi: Jdbi) {
        val sql = """
            SELECT
              t.id, t.name,
              p.id pid, p.tagId ptagId, p.primaryName pprimaryName
            FROM Tag t JOIN Product P ON t.id = p.TagId
        """.trimIndent()

        val rows = jdbi.withHandle(
            HandleCallback<List<JoinRow>, RuntimeException> { handle -> handle.createQuery(sql).mapTo<JoinRow>().list() }
        )

        assertNotNull(rows)
        assertEquals(1, rows.size)

        val joinRow = rows[0]
        assertEquals(1, joinRow[Tag::class.java].id)
        assertEquals("foo", joinRow[Tag::class.java].name)

        assertEquals(2, joinRow[Product::class.java].id, "Product::id mismatch")
        assertEquals("stuff", joinRow[Product::class.java].primaryName)
        assertEquals(1, joinRow[Product::class.java].tagId)
    }
}
