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
package org.jdbi.v3.core.kotlin.internal

import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.kotlin.internal.TypeConversionHelper.GenericThing
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

class TestTypeConversions {

    @JvmInline
    value class ValueThing(val value: Long)

    val valueThingType = ValueThing::class.java

    internal data class TestValues(
        val primitiveInt: Int,
        val string: String,
        val stringList: List<String>,
        val uuidThing: GenericThing<UUID>,
        val valueThing: ValueThing
    )

    lateinit var properties: Map<String, KProperty<*>>

    @BeforeEach
    fun setup() {
        properties = TestValues::class.memberProperties.associateBy { it.name }
    }

    @Test
    fun testSimple() {
        assertThat(toJavaType(properties["primitiveInt"]!!.returnType)).isSameAs(Integer.TYPE)
        assertThat(toJavaType(properties["string"]!!.returnType)).isSameAs(String::class.java)
    }

    @Test
    fun testGeneric() {
        assertThat(toJavaType(properties["stringList"]!!.returnType)).isEqualTo(TypeConversionHelper.GENERIC_STRING_LIST)
        assertThat(toJavaType(properties["uuidThing"]!!.returnType)).isEqualTo(TypeConversionHelper.GENERIC_THING)
    }

    @Test
    fun testValue() {
        assertThat(toJavaType(properties["valueThing"]!!.returnType)).isEqualTo(valueThingType).isNotEqualTo(java.lang.Long.TYPE)
    }
}
