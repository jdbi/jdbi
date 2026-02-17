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
import org.jdbi.core.generic.GenericType
import org.jdbi.core.kotlin.mapTo
import org.jdbi.core.kotlin.withHandleUnchecked
import org.jdbi.core.qualifier.QualifiedType
import org.jdbi.core.qualifier.Qualifier
import org.jdbi.testing.junit5.JdbiExtension
import org.jdbi.testing.junit5.internal.TestingInitializers
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

class TestIssue2751 {
    @RegisterExtension
    var h2Extension: JdbiExtension = JdbiExtension.h2()
        .withInitializer(TestingInitializers.something())
        .withPlugins(KotlinSqlObjectPlugin())

    data class TestClass(val param: List<String>)

    @Test
    fun testGenericWildcardType() {
        val genericType = object : GenericType<List<@JvmSuppressWildcards String>>() {}.type

        val ctor = TestClass::class.primaryConstructor
        val paramType = ctor!!.parameters[0].type.javaType
        assertTrue(genericType == paramType)
    }

    data class MyThing(val id: Int, @Stringy val name: List<String>)

    @Qualifier
    annotation class Stringy

    @Test
    fun testGenericMapperRegistrationSuppressOnType() {
        val jdbi = h2Extension.jdbi
        val type: QualifiedType<List<String>> = QualifiedType.of(object : GenericType<List<@JvmSuppressWildcards String>>() {})
            .with(Stringy::class.java)

        jdbi.registerColumnMapper(type, ListStringMapper())

        jdbi.useHandle<Exception> { handle -> handle.execute("INSERT INTO something(id, name) VALUES(1, 'a,b,c,d,e,f')") }

        val result: MyThing = jdbi.withHandleUnchecked { handle ->
            handle.createQuery("SELECT id, name FROM something WHERE id = 1")
                .mapTo<MyThing>()
                .one()
        }

        assertThat(result.name).isEqualTo(listOf("a", "b", "c", "d", "e", "f"))
    }
}
