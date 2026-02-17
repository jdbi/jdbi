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

package org.jdbi.core.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.jdbi.core.Handle
import org.jdbi.core.qualifier.Reversed
import org.jdbi.core.qualifier.ReversedStringArgumentFactory
import org.jdbi.core.qualifier.ReversedStringMapper
import org.jdbi.testing.junit5.JdbiExtension
import org.jdbi.testing.junit5.internal.TestingInitializers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class KotlinQualifierTest {

    @RegisterExtension
    @JvmField
    val h2Extension: JdbiExtension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(KotlinPlugin())

    private lateinit var handle: Handle

    @BeforeEach
    fun setup() {
        handle = h2Extension.sharedHandle
            .registerArgument(ReversedStringArgumentFactory())
            .registerColumnMapper(ReversedStringMapper())
    }

    @Test
    fun bindDataClassWithQualifiedConstructorParam() {
        handle.createUpdate("insert into something (id, name) values (:id, :name)")
            .bindKotlin(DataClassQualifiedConstructorParam(1, "abc"))
            .execute()

        assertThat(
            handle.select("select name from something")
                .mapTo<String>()
                .one()
        )
            .isEqualTo("cba")
    }

    @Test
    fun mapDataClassWithQualifiedConstructorParam() {
        handle.execute("insert into something (id, name) values (1, 'abc')")

        assertThat(
            handle.select("select * from something")
                .mapTo<DataClassQualifiedConstructorParam>()
                .one()
        )
            .isEqualTo(DataClassQualifiedConstructorParam(1, "cba"))
    }

    data class DataClassQualifiedConstructorParam(val id: Int, @Reversed val name: String)

    @Test
    fun bindDataClassWithQualifiedLateInitProperty() {
        handle.createUpdate("insert into something (id, name) values (:id, :name)")
            .bindKotlin(DataClassQualifiedLateInitProperty(1).also { it.name = "abc" })
            .execute()

        assertThat(
            handle.select("select name from something")
                .mapTo<String>()
                .one()
        )
            .isEqualTo("cba")
    }

    @Test
    fun mapDataClassWithQualifiedLateInitProperty() {
        handle.execute("insert into something (id, name) values (1, 'abc')")

        assertThat(
            handle.select("select * from something")
                .mapTo<DataClassQualifiedLateInitProperty>()
                .one()
        )
            .isEqualTo(DataClassQualifiedLateInitProperty(1).also { it.name = "cba" })
    }

    data class DataClassQualifiedLateInitProperty(val id: Int) {
        @Reversed
        lateinit var name: String
    }

    @Test
    fun bindDataClassWithQualifiedVarProperty() {
        handle.createUpdate("insert into something (id, name) values (:id, :name)")
            .bindKotlin(DataClassQualifiedVarProperty(1).also { it.name = "abc" })
            .execute()

        assertThat(
            handle.select("select name from something")
                .mapTo<String>()
                .one()
        )
            .isEqualTo("cba")
    }

    @Test
    fun mapDataClassWithQualifiedVarProperty() {
        handle.execute("insert into something (id, name) values (1, 'abc')")

        assertThat(
            handle.select("select * from something")
                .mapTo<DataClassQualifiedVarProperty>()
                .one()
        )
            .isEqualTo(DataClassQualifiedVarProperty(1).also { it.name = "cba" })
    }

    data class DataClassQualifiedVarProperty(val id: Int) {
        @Reversed
        var name: String? = null
    }
}
