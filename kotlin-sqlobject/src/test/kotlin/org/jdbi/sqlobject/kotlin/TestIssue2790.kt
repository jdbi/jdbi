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

import org.assertj.core.api.Assertions.assertThat
import org.jdbi.core.Handle
import org.jdbi.core.kotlin.KotlinMapperFactory
import org.jdbi.core.kotlin.bindKotlin
import org.jdbi.core.kotlin.mapTo
import org.jdbi.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.jdbi.testing.junit.JdbiExtension
import org.jdbi.testing.junit.internal.TestingInitializers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class TestIssue2790 {
    @RegisterExtension
    var h2Extension: JdbiExtension = JdbiExtension.h2()
        .withInitializer(TestingInitializers.something())
        .withPlugins(KotlinSqlObjectPlugin(installKotlinMapperFactory = false))

    private lateinit var handle: Handle

    @BeforeEach
    fun setUp() {
        handle = h2Extension.jdbi.open()
    }

    @AfterEach
    fun tearDown() {
        handle.close()
    }

    data class Something(val id: Int, val name: String)

    data class EvenMoreThings(val id: Int, val justAListOfStrings: List<String>)

    @Test
    fun testListsWork() {
        handle.registerRowMapper(KotlinMapperFactory())

        val list = listOf("a", "b", "c")
        val ktBean = EvenMoreThings(1, list)

        handle.createUpdate("INSERT INTO something(id, name) VALUES(:id, :justAListOfStrings)")
            .bindKotlin(ktBean)
            .execute()

        val result = handle.createQuery("SELECT id, name from something")
            .mapTo<Something>()
            .single()

        assertThat(result).isEqualTo(Something(1, list.toString()))
    }
}
