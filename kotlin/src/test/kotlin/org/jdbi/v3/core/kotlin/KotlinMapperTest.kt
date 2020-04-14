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
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.reflect.ColumnName
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor
import org.jdbi.v3.core.rule.H2DatabaseRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import kotlin.properties.Delegates
import kotlin.reflect.KProperty
import org.jdbi.v3.core.mapper.PropagateNull

class KotlinMapperTest {
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
        handle.execute("CREATE TABLE the_things(id integer, first text, second text, third text, fourth text)")
        handle.execute("CREATE TABLE the_other_things(id integer, other text)")
    }

    data class DataClassWithOnlyPrimaryConstructor(val id: Int, val first: String)

    @Test
    fun testDataClassWithOnlyPrimaryConstructor() {
        val expected = DataClassWithOnlyPrimaryConstructor(1, "does this work?")

        handle.createUpdate("INSERT INTO the_things(id, first) VALUES(:id, :first)")
                .bindBean(expected)
                .execute()

        val result = handle.createQuery("SELECT * from the_things")
                .mapTo<DataClassWithOnlyPrimaryConstructor>()
                .single()

        assertThat(result).isEqualTo(expected)
    }

    data class DataClassWithAnnotatedValParameter(val id: Int, @ColumnName("first") val n: String)

    @Test
    fun testDataClassWithAnnotatedValParameter() {
        val expected = DataClassWithAnnotatedValParameter(1, "does this work?")

        handle.createUpdate("INSERT INTO the_things(id, first) VALUES(:id, :n)")
                .bindBean(expected)
                .execute()

        val result = handle.createQuery("SELECT * from the_things")
                .mapTo<DataClassWithAnnotatedValParameter>()
                .single()

        assertThat(result).isEqualTo(expected)
    }

    data class DataClassWithAnnotatedVarParameter(var id: Int, @ColumnName("first") var n: String)

    @Test
    fun testDataClassWithAnnotatedVarParameter() {
        val expected = DataClassWithAnnotatedVarParameter(1, "does this work?")

        handle.createUpdate("INSERT INTO the_things(id, first) VALUES(:id, :n)")
                .bindBean(expected)
                .execute()

        val result = handle.createQuery("SELECT * from the_things")
                .mapTo<DataClassWithAnnotatedVarParameter>()
                .single()

        assertThat(result).isEqualTo(expected)
    }

    class ClassWithOnlyPrimaryConstructor(val id: Int, val first: String)

    @Test
    fun testClassWithOnlyPrimaryConstructor() {
        val expected = ClassWithOnlyPrimaryConstructor(1, "does this work?")

        handle.createUpdate("INSERT INTO the_things(id, first) VALUES(:id, :first)")
                .bindBean(expected)
                .execute()

        val result = handle.createQuery("SELECT * from the_things")
                .mapTo<ClassWithOnlyPrimaryConstructor>()
                .single()

        assertThat(result)
                .extracting("id", "first")
                .containsExactly(expected.id, expected.first)
    }

    class ClassWithOnlySecondaryConstructor {
        val id: Int
        val first: String

        constructor(id: Int, first: String) {
            this.id = id
            this.first = first
        }
    }

    @Test
    fun testClassWithOnlySecondaryConstructor() {
        val expected = ClassWithOnlySecondaryConstructor(1, "does this work?")

        handle.createUpdate("INSERT INTO the_things(id, first) VALUES(:id, :first)")
                .bindBean(expected)
                .execute()

        val result = handle.createQuery("SELECT * from the_things")
                .mapTo<ClassWithOnlySecondaryConstructor>()
                .single()

        assertThat(result)
                .extracting("id", "first")
                .containsExactly(expected.id, expected.first)
    }

    class ClassWithWritableProperty(val id: Int) {
        lateinit var first: String
    }

    @Test
    fun testClassWithWritableProperty() {
        val expected = ClassWithWritableProperty(1)
        expected.first = "does this work?"

        handle.createUpdate("INSERT INTO the_things(id, first) VALUES(:id, :first)")
                .bindBean(expected)
                .execute()

        val result = handle.createQuery("SELECT * from the_things")
                .mapTo<ClassWithWritableProperty>()
                .single()

        assertThat(result)
                .extracting("id", "first")
                .containsExactly(expected.id, expected.first)
    }

    class ClassWithAnnotatedWritableProperty(val id: Int) {
        @ColumnName("first")
        lateinit var foo: String
    }

    @Test
    fun testClassWithAnnotatedWritableProperty() {
        val expected = ClassWithAnnotatedWritableProperty(1)
        expected.foo = "does this work?"

        handle.createUpdate("INSERT INTO the_things(id, first) VALUES(:id, :first)")
                .bind("id", expected.id)
                .bind("first", expected.foo)
                .execute()

        val result = handle.createQuery("SELECT * from the_things")
                .mapTo<ClassWithAnnotatedWritableProperty>()
                .single()

        assertThat(result)
                .extracting("id", "foo")
                .containsExactly(expected.id, expected.foo)
    }

    private data class TheNestedDataClass(val other: String)
    private data class TheDataClass(val first: String, @Nested("nested_") val nested: TheNestedDataClass)

    @Test
    fun testDataClassWithNestedConstructorParameter() {
        val expected = TheDataClass("something", TheNestedDataClass("something else"))

        handle.createUpdate("INSERT INTO the_things(id, first) VALUES(1, :value)")
                .bind("value", expected.first)
                .execute()
        handle.createUpdate("INSERT INTO the_other_things(id, other) VALUES(1, :other)")
                .bind("other", expected.nested.other)
                .execute()

        val result = handle
                .createQuery("SELECT a.first AS first, b.other AS nested_other FROM the_things a JOIN the_other_things b ON a.id = b.id")
                .mapTo<TheDataClass>()
                .single()

        assertThat(result).isEqualTo(expected)
    }

    private class TheNestedClass() {
        lateinit var other: String

        constructor(other: String) : this() {
            this.other = other
        }
    }

    private class TheClass() {
        @Nested("nested_")
        lateinit var nested: TheNestedClass
        lateinit var first: String

        constructor(first: String, nested: TheNestedClass) : this() {
            this.first = first
            this.nested = nested
        }
    }

    @Test
    fun testClassWithNestedMemberProperty() {
        val expected = TheClass("something", TheNestedClass("something else"))

        handle.createUpdate("INSERT INTO the_things(id, first) VALUES(1, :value)")
                .bind("value", expected.first)
                .execute()
        handle.createUpdate("INSERT INTO the_other_things(id, other) VALUES(1, :other)")
                .bind("other", expected.nested.other)
                .execute()

        val result = handle
                .createQuery("SELECT a.first AS first, b.other AS nested_other FROM the_things a JOIN the_other_things b ON a.id = b.id")
                .mapTo<TheClass>()
                .single()

        assertThat(result)
                .extracting("first", "nested.other")
                .containsExactly(expected.first, expected.nested.other)
    }

    class TestSkipMemberIfSetViaConstructor(@ColumnName("first") foo: String) {
        val fromCtor = foo
        var first: String by Delegates.observable("NOT_SET") { _: KProperty<*>, _: String, _: String ->
            throw UnsupportedOperationException("Should not be called")
        }
    }

    @Test
    fun testSkipMemberIfSetViaConstructor() {
        val expected = "it works!"

        handle.createUpdate("INSERT INTO the_things(id, first) VALUES(1, :value)")
                .bind("value", expected)
                .execute()

        val result = handle.createQuery("SELECT first FROM the_things")
                .mapTo<TestSkipMemberIfSetViaConstructor>()
                .first()

        assertThat(result.fromCtor).isEqualTo(expected)
    }

    private val one = "one"
    private val two = "two"
    private val three = "three"
    private val four = "four"

    private fun oneTwoThreeFourSetup() {

        handle.createUpdate("INSERT INTO the_things(id, first, second, third, fourth) VALUES(1, :one, :two, :three, :four)")
            .bind("one", one)
            .bind("two", two)
            .bind("three", three)
            .bind("four", four)
            .execute()
    }

    private val oneTwoThreeFourQuery = "SELECT id, first, second, third, fourth FROM the_things"

    class ClassWithUnusedWriteableVariable(val first : String) {
        lateinit var second : String
        var third : String = "I still get written"
        var extraField : String = "unchanged"
    }

    @Test
    fun testAllowWritableUnusedVariables() {
        oneTwoThreeFourSetup()
        val result = handle.createQuery(oneTwoThreeFourQuery)
            .mapTo<ClassWithUnusedWriteableVariable>()
            .first()
        assertThat(result.first).isEqualTo(one)
        assertThat(result.second).isEqualTo(two)
        assertThat(result.third).isEqualTo(three)
        assertThat(result.extraField).isEqualTo("unchanged")
    }

    @Test
    fun testDisallowUnmappedLateInitVariables() {
        oneTwoThreeFourSetup()
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            handle.createQuery("SELECT id, first, third, fourth FROM the_things ").mapTo<ClassWithUnusedWriteableVariable>().first()
        }
    }

    class ClassWithMultipleSecondaryConstructors {
        val id: Int
        val first: String

        constructor(id: Int, first: String) {
            this.id = id
            this.first = first
        }

        constructor(id : Int, first : String, second : String) {
            this.id = id
            this.first = first + second
        }
    }

    @Test
    fun testClassWithMultipleConstructors() {
        oneTwoThreeFourSetup()

        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            handle.createQuery(oneTwoThreeFourQuery)
                .mapTo<ClassWithMultipleSecondaryConstructors>()
        }
    }

    class ClassWithPrimaryAndMultipleSecondaryConstructors(val id : Int, val first : String) {

        constructor(id : Int, first : String, second : String) : this( id, first + second) {
            throw UnsupportedOperationException("Should not be called.")
        }

        constructor(id : Int, first : String, second : String, third : String) : this (id, first+second+third) {
            throw UnsupportedOperationException("Should not be called.")
        }
    }

    @Test
    fun testClassWithPrimaryAndMultipleSecondaryConstructors() {
        oneTwoThreeFourSetup()
        val expected = "one"

        val result = handle.createQuery(oneTwoThreeFourQuery)
            .mapTo<ClassWithPrimaryAndMultipleSecondaryConstructors>()
            .first()

        assertThat(result.first).isEqualTo(expected)
    }

    class ClassWithPrimaryAndSecondaryConstructorsWithAnnotation(val id: Int, val calculated : String) {

        constructor(id : Int, first : String, second : String) : this( id, first + second) {
            throw UnsupportedOperationException("Should never be called")
        }

        @JdbiConstructor
        constructor(id : Int, first : String, second : String, third : String) : this (id, first+second+third)
    }

    @Test
    fun testClassWithPrimaryAndSecondaryConstructorsWithAnnotation() {
        val expected = one + two + three

        oneTwoThreeFourSetup()

        val result = handle.createQuery(oneTwoThreeFourQuery)
            .mapTo<ClassWithPrimaryAndSecondaryConstructorsWithAnnotation>()
            .first()

        assertThat(result.calculated).isEqualTo(expected)
    }

    class ClassWithTooManyAnnotations
    @JdbiConstructor
    constructor (val id: Int, val calculated : String) {

        constructor(id : Int, first : String, second : String) : this( id, first + second) {
            throw UnsupportedOperationException("Should never be called")
        }

        @JdbiConstructor
        constructor(id : Int, first : String, second : String, third : String) : this (id, first+second+third)
    }

    @Test
    fun testClassWithTooManyAnnotations() {
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            handle.createQuery(oneTwoThreeFourQuery)
                .mapTo<ClassWithTooManyAnnotations>()
                .first()
        }
    }

    enum class KotlinTestEnum {
        A,B,C
    }

    @Test
    fun testKotlinMapperSkipsKotlinEnums() {
        // https://github.com/jdbi/jdbi/issues/1218
        val values = KotlinTestEnum.values()

        values.forEachIndexed { index, kotlinTestEnum ->
            handle.createUpdate("INSERT INTO the_things(id, first) VALUES(:id, :value)")
                .bind("id", index)
                .bind("value", kotlinTestEnum)
                .execute()
        }

        val result = handle.createQuery("SELECT first FROM the_things")
            .mapTo<KotlinTestEnum>()
            .list()

        assertThat(result.size).isEqualTo(values.size)
        assertThat(result).containsAll(values.asList())
    }

    data class DataClassWithNullableConstructorParameter(val id:Int,
                                                         val name:String?)

    @Test
    fun testDataClassWithNullableConstructorParameter() {
        assertThat(handle.select("select 1 as id")
            .mapTo<DataClassWithNullableConstructorParameter>()
            .first())
            .isEqualTo(DataClassWithNullableConstructorParameter(1, null))

        assertThat(handle.select("select 1 as id, 'foo' as name")
            .mapTo<DataClassWithNullableConstructorParameter>()
            .first())
            .isEqualTo(DataClassWithNullableConstructorParameter(1, "foo"))
    }

    class ClassWithNullableProperty(val id: Int) {
        var name:String? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ClassWithNullableProperty

            if (id != other.id) return false
            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + (name?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "DataClassWithNullableProperty(id=$id, name=$name)"
        }
    }

    @Test
    fun testClassWithNullableProperty() {
        assertThat(handle.select("select 1 as id")
            .mapTo<ClassWithNullableProperty>()
            .first())
            .isEqualTo(ClassWithNullableProperty(1))

        assertThat(handle.select("select 1 as id, 'foo' as name")
            .mapTo<ClassWithNullableProperty>()
            .first())
            .isEqualTo(ClassWithNullableProperty(1).also { it.name = "foo" })
    }

    data class NestedDataClass(val foo:String,
                               val bar:String?)

    data class DataClassWithNullableNestedConstructorParameter(val id: Int,
                                                               @Nested val nested: NestedDataClass?)

    @Test
    fun testDataClassWithNullableNestedConstructorParameter() {
        assertThat(handle.select("select 1 as id")
            .mapTo<DataClassWithNullableNestedConstructorParameter>()
            .first())
            .isEqualTo(DataClassWithNullableNestedConstructorParameter(1, null))

        assertThat(handle.select("select 1 as id, 'foo' as foo")
            .mapTo<DataClassWithNullableNestedConstructorParameter>()
            .first())
            .isEqualTo(DataClassWithNullableNestedConstructorParameter(1, NestedDataClass("foo", null)))

        assertThat(handle.select("select 1 as id, 'foo' as foo, 'bar' as bar")
            .mapTo<DataClassWithNullableNestedConstructorParameter>()
            .first())
            .isEqualTo(DataClassWithNullableNestedConstructorParameter(1, NestedDataClass("foo", "bar")))
    }

    class ClassWithNullableNestedProperty(val id: Int) {
        @Nested var nested: NestedDataClass? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ClassWithNullableNestedProperty

            if (id != other.id) return false
            if (nested != other.nested) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + (nested?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "ClassWithNullableNestedProperty(id='$id', nested=$nested)"
        }
    }

    @Test
    fun testClassWithNullableNestedProperty() {
        assertThat(handle.select("select 1 as id")
            .mapTo<ClassWithNullableNestedProperty>()
            .first())
            .isEqualTo(ClassWithNullableNestedProperty(1))

        assertThat(handle.select("select 1 as id, 'foo' as foo")
            .mapTo<ClassWithNullableNestedProperty>()
            .first())
            .isEqualTo(ClassWithNullableNestedProperty(1).also { it.nested = NestedDataClass("foo", null) })

        assertThat(handle.select("select 1 as id, 'foo' as foo, 'bar' as bar")
            .mapTo<ClassWithNullableNestedProperty>()
            .first())
            .isEqualTo(ClassWithNullableNestedProperty(1).also { it.nested = NestedDataClass("foo", "bar") })
    }

    @PropagateNull("name")
    class ClassWithPropagateNull(val id: Int) {
        var name:String? = null
    }

    @Test
    fun classPropagateNull() {
        assertThat(handle.select("select 1 as id, null as name")
                .mapTo<ClassWithPropagateNull>()
                .one())
            .isNull()
    }

    class MemberWithPropagateNull(val id: Int) {
        @PropagateNull
        var name:String? = null
    }

    @Test
    fun memberPropagateNull() {
        assertThat(handle.select("select 1 as id, null as name")
                .mapTo<MemberWithPropagateNull>()
                .one())
            .isNull()
    }

    data class ConstructorWithPropagateNull(val id: Int, @PropagateNull var name:String?)

    @Test
    fun constructorPropagateNull() {
        assertThat(handle.select("select 1 as id, null as name")
                .mapTo<ConstructorWithPropagateNull>()
                .one())
            .isNull()
    }

    data class ConstructorWithDefaultParameter(val id: Int, var other: String = "default")

    @Test
    fun constructorWithDefaultParameter() {
        assertThat(handle.select("select 1 as id, null as other")
                .mapTo<ConstructorWithDefaultParameter>()
                .one())
            .isEqualTo(ConstructorWithDefaultParameter(1))

        assertThat(handle.select("select 1 as id, 'non-default' as other")
                .mapTo<ConstructorWithDefaultParameter>()
                .one())
            .isEqualTo(ConstructorWithDefaultParameter(1, "non-default"))
    }
}
