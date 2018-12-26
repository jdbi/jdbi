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
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.qualifier.Reversed
import org.jdbi.v3.core.qualifier.ReversedStringArgumentFactory
import org.jdbi.v3.core.qualifier.ReversedStringMapper
import org.jdbi.v3.core.rule.H2DatabaseRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

class KotlinQualifierTest {
    @Rule
    @JvmField
    val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Rule
    @JvmField
    val dbRule: H2DatabaseRule = H2DatabaseRule().withPlugin(KotlinPlugin())

    private lateinit var handle: Handle

    @Before
    fun setup() {
        handle = dbRule.sharedHandle
            .registerArgument(ReversedStringArgumentFactory())
            .registerColumnMapper(ReversedStringMapper())
    }

    @Test
    fun bindDataClassWithQualifiedConstructorParam() {
        handle.createUpdate("insert into something (id, name) values (:id, :name)")
            .bindKotlin(DataClassQualifiedConstructorParam(1, "abc"))
            .execute()

        assertThat(handle.select("select name from something")
            .mapTo<String>()
            .findOnly())
            .isEqualTo("cba")
    }

    @Test
    fun mapDataClassWithQualifiedConstructorParam() {
        handle.execute("insert into something (id, name) values (1, 'abc')")

        assertThat(handle.select("select * from something")
            .mapTo<DataClassQualifiedConstructorParam>()
            .findOnly())
            .isEqualTo(DataClassQualifiedConstructorParam(1, "cba"))
    }

    data class DataClassQualifiedConstructorParam(val id: Int,
                                                  @Reversed val name: String)

    @Test
    fun bindDataClassWithQualifiedLateInitProperty() {
        handle.createUpdate("insert into something (id, name) values (:id, :name)")
            .bindKotlin(DataClassQualifiedLateInitProperty(1).also { it.name = "abc" })
            .execute()

        assertThat(handle.select("select name from something")
            .mapTo<String>()
            .findOnly())
            .isEqualTo("cba")
    }

    @Test
    fun mapDataClassWithQualifiedLateInitProperty() {
        handle.execute("insert into something (id, name) values (1, 'abc')")

        assertThat(handle.select("select * from something")
            .mapTo<DataClassQualifiedLateInitProperty>()
            .findOnly())
            .isEqualTo(DataClassQualifiedLateInitProperty(1).also { it.name = "cba" })
    }

    data class DataClassQualifiedLateInitProperty(val id:Int) {
        @Reversed lateinit var name: String
    }

    @Test
    fun bindDataClassWithQualifiedVarProperty() {
        handle.createUpdate("insert into something (id, name) values (:id, :name)")
            .bindKotlin(DataClassQualifiedVarProperty(1).also { it.name = "abc" })
            .execute()

        assertThat(handle.select("select name from something")
            .mapTo<String>()
            .findOnly())
            .isEqualTo("cba")
    }

    @Test
    fun mapDataClassWithQualifiedVarProperty() {
        handle.execute("insert into something (id, name) values (1, 'abc')")

        assertThat(handle.select("select * from something")
            .mapTo<DataClassQualifiedVarProperty>()
            .findOnly())
            .isEqualTo(DataClassQualifiedVarProperty(1).also { it.name = "cba" })
    }

    data class DataClassQualifiedVarProperty(val id:Int) {
        @Reversed var name: String? = null
    }
}
