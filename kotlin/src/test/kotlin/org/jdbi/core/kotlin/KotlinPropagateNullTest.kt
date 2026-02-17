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
import org.jdbi.core.mapper.AbstractPropagateNullTest
import org.jdbi.core.mapper.Nested
import org.jdbi.core.mapper.PropagateNull
import org.jdbi.core.mapper.PropagateNullTest.Test21Bean
import org.jdbi.core.mapper.PropagateNullTest.Test21FKBean
import org.jdbi.core.mapper.PropagateNullTest.Test22Bean
import org.jdbi.core.mapper.PropagateNullTest.Test22FKBean
import org.jdbi.core.mapper.PropagateNullTest.Test23Bean
import org.jdbi.core.mapper.PropagateNullTest.Test24Bean
import org.jdbi.core.mapper.PropagateNullTest.Test24FKBean
import org.jdbi.core.mapper.PropagateNullTest.Test25Bean
import org.jdbi.core.mapper.PropagateNullTest.Test25FKBean
import org.jdbi.core.mapper.PropagateNullTest.Test26Bean
import org.jdbi.core.mapper.reflect.ColumnName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KotlinPropagateNullTest : AbstractPropagateNullTest() {

    override fun getPlugin() = KotlinPlugin()

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean does not use a prefix and the column in the nested bean is present in the
     * result set.
     */
    @Test
    fun testPropagateNullOnNestedBean() {
        propagateNullOnNested { q -> q.mapTo<Test1Bean>() }

        // also test the field bean from the Java test
        handle.registerRowMapper(KotlinMapper(Test21Bean::class))
        propagateNullOnNested { q -> q.mapTo<Test21Bean>() }
    }

    data class Test1Bean(@Nested private val nestedBean: NestedBean?) : TestBean {
        override fun getNestedBean(): NestedBean? = nestedBean

        @PropagateNull("nestedid")
        data class NestedBean(@ColumnName("nestedid") private val id: String?) : TestBean.NestedBean {
            override fun getId(): String? = id
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean does not use a prefix and the column in the nested bean is present in the
     * result set.
     */
    @Test
    fun testPropagateNullOnNestedBeanWithFK() {
        propagateNullOnNestedWithFK { q -> q.mapTo<Test1FKBean>() }

        // also test the field bean from the Java test
        handle.registerRowMapper(KotlinMapper(Test21FKBean::class))
        propagateNullOnNestedWithFK { q -> q.mapTo<Test21FKBean>() }
    }

    data class Test1FKBean(@Nested private val nestedBean: NestedBean?) : TestBean {
        override fun getNestedBean(): NestedBean? = nestedBean

        @PropagateNull("nestedfk")
        data class NestedBean(@ColumnName("nestedid") private val id: String?) : TestBean.NestedBean {
            override fun getId(): String? = id
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the snake_case mapper (parses prefixes)
     */
    @Test
    fun testPropagateNullOnNestedBeanWithPrefixSnakeCase() {
        propagateNullOnNestedColumn { q -> q.mapTo<Test2Bean>() }

        // also test the field bean from the Java test
        handle.registerRowMapper(KotlinMapper(Test22Bean::class))
        propagateNullOnNestedColumn { q -> q.mapTo<Test22Bean>() }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the case-insensitive name mapper.
     */
    @Test
    fun testPropagateNullOnNestedBeanWithPrefixCaseInsensitive() {
        testPropagateNullOnNestedWithPrefixCaseInsensitive { q -> q.mapTo<Test2Bean>() }

        // also test the field bean from the Java test
        handle.registerRowMapper(KotlinMapper(Test22Bean::class))
        testPropagateNullOnNestedWithPrefixCaseInsensitive { q -> q.mapTo<Test22Bean>() }
    }

    data class Test2Bean(@Nested("bean") private val nestedBean: NestedBean?) : TestBean {
        override fun getNestedBean(): NestedBean? = nestedBean

        @PropagateNull("id")
        data class NestedBean(private val id: String?) : TestBean.NestedBean {
            override fun getId(): String? = id
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the snake_case mapper (parses prefixes)
     */
    @Test
    fun testPropagateNullOnNestedBeanWithPrefixSnakeCaseWithFK() {
        propagateNullOnNestedColumnWithFK { q -> q.mapTo<Test2FKBean>() }

        // also test the field bean from the Java test
        handle.registerRowMapper(KotlinMapper(Test22FKBean::class))
        propagateNullOnNestedColumnWithFK { q -> q.mapTo<Test22FKBean>() }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the case-insensitive name mapper.
     */
    @Test
    fun testPropagateNullOnNestedBeanWithPrefixCaseInsensitiveWithFK() {
        testPropagateNullOnNestedWithPrefixCaseInsensitiveWithFK { q -> q.mapTo<Test2FKBean>() }

        // also test the field bean from the Java test
        handle.registerRowMapper(KotlinMapper(Test22FKBean::class))
        testPropagateNullOnNestedWithPrefixCaseInsensitiveWithFK { q -> q.mapTo<Test22FKBean>() }
    }

    data class Test2FKBean(@Nested("bean") private val nestedBean: NestedBean?) : TestBean {
        override fun getNestedBean(): NestedBean? = nestedBean

        @PropagateNull("fk")
        data class NestedBean(private val id: String?) : TestBean.NestedBean {
            override fun getId(): String? = id
        }
    }

    /**
     * Test that the propagateNull annotation on a specific column in the nested bean works. The annotation is picked up from the column (not from the bean).
     */
    @Test
    fun testPropagateNullOnNestedBeanColumn() {
        propagateNullOnNestedColumn { q -> q.mapTo<Test3Bean>() }

        // also test the field bean from the Java test
        handle.registerRowMapper(KotlinMapper(Test23Bean::class))
        propagateNullOnNestedColumn { q -> q.mapTo<Test23Bean>() }
    }

    data class Test3Bean(@Nested("bean") private val nestedBean: NestedBean?) : TestBean {
        override fun getNestedBean(): NestedBean? = nestedBean

        data class NestedBean(@PropagateNull private val id: String?) : TestBean.NestedBean {
            override fun getId(): String? = id
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Use an
     * explicit prefix for the nested bean ('n').
     */
    @Test
    fun testDoubleNestedBeanPropagateNull() {
        doubleNestedPropagateNull { q -> q.mapTo<Test4Bean>() }

        // also test the field bean from the Java test
        handle.registerRowMapper(KotlinMapper(Test24Bean::class))
        doubleNestedPropagateNull { q -> q.mapTo<Test24Bean>() }
    }

    data class Test4Bean(
        @PropagateNull
        @Nested("n")
        private val nestedBean: NestedBean?
    ) : TestBean {
        override fun getNestedBean(): NestedBean? = nestedBean

        data class NestedBean(@PropagateNull private val id: String?) : TestBean.NestedBean {
            override fun getId(): String? = id
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Use an
     * explicit prefix for the nested bean ('n').
     */
    @Test
    fun testDoubleNestedBeanPropagateNullWithFK() {
        doubleNestedPropagateNullWithFK { q -> q.mapTo<Test4FKBean>() }

        // also test the field bean from the Java test
        handle.registerRowMapper(KotlinMapper(Test24FKBean::class))
        doubleNestedPropagateNullWithFK { q -> q.mapTo<Test24FKBean>() }
    }

    data class Test4FKBean(
        @PropagateNull
        @Nested("n")
        private val nestedBean: NestedBean?
    ) : TestBean {
        override fun getNestedBean(): NestedBean? = nestedBean

        @PropagateNull("fk")
        data class NestedBean(private val id: String?) : TestBean.NestedBean {
            override fun getId(): String? = id
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Do not
     * actually set a prefix for the nested bean, so the properties are picked up without a prefix.
     */
    @Test
    fun testDoubleNestedBeanPropagateNullWithImplicitPrefix() {
        doubleNestedPropagateNull { q -> q.mapTo<Test5Bean>() }

        // also test the field bean from the Java test
        handle.registerRowMapper(KotlinMapper(Test25Bean::class))
        doubleNestedPropagateNull { q -> q.mapTo<Test25Bean>() }
    }

    data class Test5Bean(
        @PropagateNull
        @Nested
        private val nestedBean: NestedBean?
    ) : TestBean {
        override fun getNestedBean(): NestedBean? = nestedBean

        data class NestedBean(
            @PropagateNull
            @ColumnName("nid")
            private val id: String?
        ) : TestBean.NestedBean {
            override fun getId(): String? = id
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Do not
     * actually set a prefix for the nested bean, so the properties are picked up without a prefix.
     */
    @Test
    fun testDoubleNestedBeanPropagateNullWithImplicitPrefixWithFK() {
        doubleNestedPropagateNullWithFK { q -> q.mapTo<Test5FKBean>() }

        // also test the field bean from the Java test
        handle.registerRowMapper(KotlinMapper(Test25FKBean::class))
        doubleNestedPropagateNullWithFK { q -> q.mapTo<Test25FKBean>() }
    }

    data class Test5FKBean(
        @PropagateNull
        @Nested
        private val nestedBean: NestedBean?
    ) : TestBean {
        override fun getNestedBean(): NestedBean? = nestedBean

        @PropagateNull("nfk")
        data class NestedBean(@ColumnName("nid") private val id: String?) : TestBean.NestedBean {
            override fun getId(): String? = id
        }
    }

    @Test
    fun testBadPropagateNullAnnotationOnBean() {
        val e = assertThrows<IllegalArgumentException> {
            testPropagateNullOnNestedWithPrefixCaseInsensitive { q -> q.mapTo<Test6Bean>() }
        }
        assertThat(e.message).containsIgnoringCase("@PropagateNull does not support a value (id)")
        assertThat(e.message).containsIgnoringCase("nestedBean")

        handle.registerRowMapper(KotlinMapper(Test26Bean::class))
        val e2 = assertThrows<IllegalArgumentException> {
            testPropagateNullOnNestedWithPrefixCaseInsensitive { q -> q.mapTo<Test26Bean>() }
        }
        assertThat(e2.message).containsIgnoringCase("@PropagateNull does not support a value (id)")
        assertThat(e2.message).containsIgnoringCase("nestedBean")
    }

    data class Test6Bean(
        @PropagateNull("id")
        @Nested
        private val nestedBean: NestedBean?
    ) : TestBean {
        override fun getNestedBean(): NestedBean? = nestedBean

        data class NestedBean(private val id: String?) : TestBean.NestedBean {
            override fun getId(): String? = id
        }
    }
}
