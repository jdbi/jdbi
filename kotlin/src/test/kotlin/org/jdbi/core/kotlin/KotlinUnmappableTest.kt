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
import org.jdbi.core.annotation.JdbiProperty
import org.jdbi.core.internal.testing.H2DatabaseExtension
import org.jdbi.core.internal.testing.H2DatabaseExtension.SOMETHING_INITIALIZER
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class KotlinUnmappableTest {

    @RegisterExtension
    @JvmField
    val h2Extension: H2DatabaseExtension = H2DatabaseExtension.instance().withPlugin(KotlinPlugin()).withInitializer(SOMETHING_INITIALIZER)

    @Test
    fun testUnmappableOnNestedBean() {
        val handle = h2Extension.sharedHandle
        val testBean = handle.select("select 1 as id, 2 as unmapped").mapTo<TestBean>().one()

        assertThat(testBean).isNotNull
        assertThat(testBean.id).isOne
        assertThat(testBean.unmapped).isZero
    }

    data class TestBean(val id: Int = 0) {
        @JdbiProperty(map = false)
        var unmapped: Int = 0
    }
}
