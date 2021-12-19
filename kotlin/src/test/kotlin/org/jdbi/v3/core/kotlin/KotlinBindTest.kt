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

import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder
import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.generic.GenericType
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class KotlinBindTest {

    companion object {
        @RegisterExtension
        val pg = MultiDatabaseBuilder.instanceWithDefaults().build()
    }

    @RegisterExtension
    @JvmField
    val pgExtension: JdbiExtension = JdbiExtension.postgres(pg).withPlugin(KotlinPlugin())

    @Test
    fun bindEnumKotlinList() {
        assertThat(
            pgExtension.openHandle()
                .createQuery("select :echo")
                .bindArray("echo", MyEnum::class.java, listOf(MyEnum.A, MyEnum.B))
                .mapTo(object : GenericType<List<MyEnum>>() {})
                .one()
        )
            .containsExactly(MyEnum.A, MyEnum.B)
    }

    enum class MyEnum {
        A, B, C
    }
}
