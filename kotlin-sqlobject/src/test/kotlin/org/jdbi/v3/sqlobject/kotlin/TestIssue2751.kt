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

import org.assertj.core.api.Assertions.assertThat
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.generic.GenericType
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.kotlin.withHandleUnchecked
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.mapper.NoSuchMapperException
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.core.qualifier.Qualifier
import org.jdbi.v3.core.result.UnableToProduceResultException
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.SingleValue
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.testing.junit5.JdbiExtension
import org.jdbi.v3.testing.junit5.internal.TestingInitializers
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.RegisterExtension
import java.sql.ResultSet
import kotlin.reflect.KClass
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

    class Bounded<out A, out B : A>

    data class BoundedCtor(val id: Int, val name: Bounded<Number, Int>)

    @Test
    fun testBoundDependentTypeArguments() {
        val sentinel = Bounded<Number, Int>()
        val jdbi = h2Extension.jdbi
        jdbi.registerColumnMapper(object : GenericType<Bounded<Number, Int>>() {}, ColumnMapper { _, _, _ -> sentinel })
        jdbi.useHandle<Exception> { handle -> handle.execute("INSERT INTO something(id, name) VALUES(1, 'red')") }

        assertThat(mapRow(jdbi, BoundedCtor::class).name).isSameAs(sentinel)
    }

    @Test
    fun testCollectIntoWithElementMapper() {
        val sentinel = Code("sentinel")
        // without the Kotlin plugin, whose KotlinMapper row mapper takes precedence over a column mapper for a data class
        val jdbi = Jdbi.create(h2Extension.url)
        jdbi.registerColumnMapper(Code::class.java, ColumnMapper { _, _, _ -> sentinel })
        jdbi.useHandle<Exception> { handle -> handle.execute("INSERT INTO something(id, name) VALUES(1, 'red')") }

        val result = jdbi.withHandleUnchecked { handle ->
            handle.createQuery(NAME_QUERY).collectInto(object : GenericType<List<Code>>() {})
        }

        assertThat(result).containsExactly(sentinel)
    }

    interface Tag

    data class TagImpl(val value: String) : Tag

    sealed class Shape

    data class Circle(val value: String) : Shape()

    data class Code(val value: String)

    enum class Color { RED, BLUE }

    enum class Mood {
        HAPPY {
            override fun label() = "happy"
        },
        SAD {
            override fun label() = "sad"
        };

        abstract fun label(): String
    }

    data class TagCtor(val id: Int, val name: List<Tag>)

    data class ShapeCtor(val id: Int, val name: List<Shape>)

    data class MoodCtor(val id: Int, val name: List<Mood>)

    data class NestedCtor(val id: Int, val name: List<List<String>>)

    data class CodeCtor(val id: Int, val name: List<Code>)

    data class ColorCtor(val id: Int, val name: List<Color>)

    data class StringCtor(val id: Int, val name: List<String>)

    data class IntArrayCtor(val id: Int, val name: List<IntArray>)

    data class StringArrayCtor(val id: Int, val name: List<Array<String>>)

    class TagVar {
        var id: Int = 0
        lateinit var name: List<Tag>
    }

    class ShapeVar {
        var id: Int = 0
        lateinit var name: List<Shape>
    }

    class MoodVar {
        var id: Int = 0
        lateinit var name: List<Mood>
    }

    class NestedVar {
        var id: Int = 0
        lateinit var name: List<List<String>>
    }

    class CodeVar {
        var id: Int = 0
        lateinit var name: List<Code>
    }

    class ColorVar {
        var id: Int = 0
        lateinit var name: List<Color>
    }

    class StringVar {
        var id: Int = 0
        lateinit var name: List<String>
    }

    class IntArrayVar {
        var id: Int = 0
        lateinit var name: List<IntArray>
    }

    class StringArrayVar {
        var id: Int = 0
        lateinit var name: List<Array<String>>
    }

    interface ListDao {
        @SqlQuery(NAME_QUERY)
        @SingleValue
        fun tags(): List<Tag>

        @SqlQuery(NAME_QUERY)
        @SingleValue
        fun shapes(): List<Shape>

        @SqlQuery(NAME_QUERY)
        @SingleValue
        fun moods(): List<Mood>

        @SqlQuery(NAME_QUERY)
        @SingleValue
        fun nested(): List<List<String>>

        @SqlQuery(NAME_QUERY)
        @SingleValue
        fun codes(): List<Code>

        @SqlQuery(NAME_QUERY)
        @SingleValue
        fun colors(): List<Color>

        @SqlQuery(NAME_QUERY)
        @SingleValue
        fun strings(): List<String>

        @SqlQuery(NAME_QUERY)
        @SingleValue
        fun intArrays(): List<IntArray>

        @SqlQuery(NAME_QUERY)
        @SingleValue
        fun stringArrays(): List<Array<String>>
    }

    enum class Lookup { CONSTRUCTOR_PARAMETER, VAR_PROPERTY, SQL_OBJECT_RETURN }

    enum class Registration { GENERIC_TYPE, SUPPRESSED_WILDCARDS, INFERRED_FROM_MAPPER }

    class Case(
        val name: String,
        val sentinel: List<Any>,
        // the GenericType without and with @JvmSuppressWildcards
        val genericTypes: Pair<GenericType<*>, GenericType<*>>,
        val inferred: ColumnMapper<*>,
        val lookups: Map<Lookup, (Jdbi) -> Any?>,
        val miss: ((Lookup) -> Any)? = null
    ) {
        fun register(jdbi: Jdbi, registration: Registration) {
            when (registration) {
                Registration.GENERIC_TYPE -> jdbi.registerColumnMapper(genericTypes.first.type, ColumnMapper { _, _, _ -> sentinel })
                Registration.SUPPRESSED_WILDCARDS -> jdbi.registerColumnMapper(genericTypes.second.type, ColumnMapper { _, _, _ -> sentinel })
                Registration.INFERRED_FROM_MAPPER -> jdbi.registerColumnMapper(inferred)
            }
        }
    }

    private fun <T : Any> mapRow(jdbi: Jdbi, type: KClass<T>): T = jdbi.withHandleUnchecked { handle ->
        handle.createQuery("SELECT id, name FROM something WHERE id = 1").mapTo(type.java).one()
    }

    private fun dao(jdbi: Jdbi) = jdbi.onDemand(ListDao::class.java)

    private fun tagCase(): Case {
        val sentinel = listOf<Tag>(TagImpl("sentinel"))
        return Case(
            "interface",
            sentinel,
            object : GenericType<List<Tag>>() {} to object : GenericType<List<@JvmSuppressWildcards Tag>>() {},
            object : ColumnMapper<List<Tag>> {
                override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext) = sentinel
            },
            mapOf(
                Lookup.CONSTRUCTOR_PARAMETER to { jdbi -> mapRow(jdbi, TagCtor::class).name },
                Lookup.VAR_PROPERTY to { jdbi -> mapRow(jdbi, TagVar::class).name },
                Lookup.SQL_OBJECT_RETURN to { jdbi -> dao(jdbi).tags() }
            ),
            ::noMapperFound
        )
    }

    private fun shapeCase(): Case {
        val sentinel = listOf<Shape>(Circle("sentinel"))
        return Case(
            "sealed class",
            sentinel,
            object : GenericType<List<Shape>>() {} to object : GenericType<List<@JvmSuppressWildcards Shape>>() {},
            object : ColumnMapper<List<Shape>> {
                override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext) = sentinel
            },
            mapOf(
                Lookup.CONSTRUCTOR_PARAMETER to { jdbi -> mapRow(jdbi, ShapeCtor::class).name },
                Lookup.VAR_PROPERTY to { jdbi -> mapRow(jdbi, ShapeVar::class).name },
                Lookup.SQL_OBJECT_RETURN to { jdbi -> dao(jdbi).shapes() }
            ),
            ::noMapperFound
        )
    }

    private fun moodCase(): Case {
        val sentinel = listOf(Mood.SAD)
        return Case(
            "enum with constant bodies",
            sentinel,
            object : GenericType<List<Mood>>() {} to object : GenericType<List<@JvmSuppressWildcards Mood>>() {},
            object : ColumnMapper<List<Mood>> {
                override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext) = sentinel
            },
            mapOf(
                Lookup.CONSTRUCTOR_PARAMETER to { jdbi -> mapRow(jdbi, MoodCtor::class).name },
                Lookup.VAR_PROPERTY to { jdbi -> mapRow(jdbi, MoodVar::class).name },
                Lookup.SQL_OBJECT_RETURN to { jdbi -> dao(jdbi).moods() }
            ),
            { UnableToProduceResultException::class }
        )
    }

    private fun nestedCase(): Case {
        val sentinel = listOf(listOf("sentinel"))
        return Case(
            "nested list",
            sentinel,
            object : GenericType<List<List<String>>>() {} to object : GenericType<List<@JvmSuppressWildcards List<String>>>() {},
            object : ColumnMapper<List<List<String>>> {
                override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext) = sentinel
            },
            mapOf(
                Lookup.CONSTRUCTOR_PARAMETER to { jdbi -> mapRow(jdbi, NestedCtor::class).name },
                Lookup.VAR_PROPERTY to { jdbi -> mapRow(jdbi, NestedVar::class).name },
                Lookup.SQL_OBJECT_RETURN to { jdbi -> dao(jdbi).nested() }
            ),
            { lookup -> if (lookup == Lookup.CONSTRUCTOR_PARAMETER) IllegalArgumentException::class else listOf(listOf("red")) }
        )
    }

    private fun codeCase(): Case {
        val sentinel = listOf(Code("sentinel"))
        return Case(
            "data class",
            sentinel,
            object : GenericType<List<Code>>() {} to object : GenericType<List<@JvmSuppressWildcards Code>>() {},
            object : ColumnMapper<List<Code>> {
                override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext) = sentinel
            },
            mapOf(
                Lookup.CONSTRUCTOR_PARAMETER to { jdbi -> mapRow(jdbi, CodeCtor::class).name },
                Lookup.VAR_PROPERTY to { jdbi -> mapRow(jdbi, CodeVar::class).name },
                Lookup.SQL_OBJECT_RETURN to { jdbi -> dao(jdbi).codes() }
            )
        )
    }

    private fun colorCase(): Case {
        val sentinel = listOf(Color.BLUE)
        return Case(
            "enum",
            sentinel,
            object : GenericType<List<Color>>() {} to object : GenericType<List<@JvmSuppressWildcards Color>>() {},
            object : ColumnMapper<List<Color>> {
                override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext) = sentinel
            },
            mapOf(
                Lookup.CONSTRUCTOR_PARAMETER to { jdbi -> mapRow(jdbi, ColorCtor::class).name },
                Lookup.VAR_PROPERTY to { jdbi -> mapRow(jdbi, ColorVar::class).name },
                Lookup.SQL_OBJECT_RETURN to { jdbi -> dao(jdbi).colors() }
            )
        )
    }

    private fun stringCase(): Case {
        val sentinel = listOf("sentinel")
        return Case(
            "String",
            sentinel,
            object : GenericType<List<String>>() {} to object : GenericType<List<@JvmSuppressWildcards String>>() {},
            object : ColumnMapper<List<String>> {
                override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext) = sentinel
            },
            mapOf(
                Lookup.CONSTRUCTOR_PARAMETER to { jdbi -> mapRow(jdbi, StringCtor::class).name },
                Lookup.VAR_PROPERTY to { jdbi -> mapRow(jdbi, StringVar::class).name },
                Lookup.SQL_OBJECT_RETURN to { jdbi -> dao(jdbi).strings() }
            )
        )
    }

    private fun intArrayCase(): Case {
        val sentinel = listOf(intArrayOf(7))
        return Case(
            "IntArray",
            sentinel,
            object : GenericType<List<IntArray>>() {} to object : GenericType<List<@JvmSuppressWildcards IntArray>>() {},
            object : ColumnMapper<List<IntArray>> {
                override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext) = sentinel
            },
            mapOf(
                Lookup.CONSTRUCTOR_PARAMETER to { jdbi -> mapRow(jdbi, IntArrayCtor::class).name },
                Lookup.VAR_PROPERTY to { jdbi -> mapRow(jdbi, IntArrayVar::class).name },
                Lookup.SQL_OBJECT_RETURN to { jdbi -> dao(jdbi).intArrays() }
            )
        )
    }

    private fun stringArrayCase(): Case {
        val sentinel = listOf(arrayOf("sentinel"))
        return Case(
            "Array<String>",
            sentinel,
            object : GenericType<List<Array<String>>>() {} to object : GenericType<List<@JvmSuppressWildcards Array<String>>>() {},
            object : ColumnMapper<List<Array<String>>> {
                override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext) = sentinel
            },
            mapOf(
                Lookup.CONSTRUCTOR_PARAMETER to { jdbi -> mapRow(jdbi, StringArrayCtor::class).name },
                Lookup.VAR_PROPERTY to { jdbi -> mapRow(jdbi, StringArrayVar::class).name },
                Lookup.SQL_OBJECT_RETURN to { jdbi -> dao(jdbi).stringArrays() }
            )
        )
    }

    private fun cases() = listOf(tagCase(), shapeCase(), moodCase(), nestedCase(), codeCase(), colorCase(), stringCase(), intArrayCase(), stringArrayCase())

    private fun expectMatch(case: Case, registration: Registration, lookup: Lookup): Boolean {
        val suppressed = registration == Registration.SUPPRESSED_WILDCARDS
        return case.miss == null || suppressed == (lookup != Lookup.CONSTRUCTOR_PARAMETER)
    }

    /**
     * The mappers return a sentinel that the database row can not produce, so a match proves the registered mapper ran.
     * A miss either fails or lets a built-in mapper answer: the enum mapper fails on the row, and the SQL array mapper
     * returns the row as a nested list without an error.
     * Kotlin keeps the wildcard on an open element type in a parameter signature but never writes one into a return type,
     * so for an open element type the matching registration depends on the lookup path.
     */
    @TestFactory
    fun testGenericTypeRegistrationMatrix(): List<DynamicTest> {
        h2Extension.jdbi.useHandle<Exception> { handle -> handle.execute("INSERT INTO something(id, name) VALUES(1, 'red')") }

        return cases().flatMap { case ->
            Registration.entries.flatMap { registration ->
                Lookup.entries.map { lookup ->
                    val match = expectMatch(case, registration, lookup)
                    DynamicTest.dynamicTest("${case.name}, $registration, $lookup, matches: $match") {
                        val jdbi = Jdbi.create(h2Extension.url).installPlugin(KotlinSqlObjectPlugin())
                        case.register(jdbi, registration)
                        val result = runCatching { case.lookups.getValue(lookup)(jdbi) }
                        if (match) {
                            assertThat(result.getOrThrow()).isEqualTo(case.sentinel)
                        } else {
                            when (val expected = case.miss!!(lookup)) {
                                IllegalArgumentException::class ->
                                    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
                                        .hasMessageContaining("Could not find column mapper")
                                is KClass<*> -> assertThat(result.exceptionOrNull()).isInstanceOf(expected.java)
                                else -> assertThat(result.getOrThrow()).isEqualTo(expected)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val NAME_QUERY = "SELECT name FROM something WHERE id = 1"
    }
}

private fun noMapperFound(lookup: TestIssue2751.Lookup) =
    if (lookup == TestIssue2751.Lookup.SQL_OBJECT_RETURN) NoSuchMapperException::class else IllegalArgumentException::class
