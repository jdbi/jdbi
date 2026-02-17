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
package org.jdbi.core.mapper;

import org.jdbi.core.mapper.reflect.BeanMapper;
import org.jdbi.core.mapper.reflect.ColumnName;
import org.jdbi.core.mapper.reflect.ConstructorMapper;
import org.jdbi.core.mapper.reflect.FieldMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PropagateNullTest extends AbstractPropagateNullTest {

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean does not use a prefix and the column in the nested bean is present in the
     * result set.
     */
    @Test
    void testPropagateNullOnNestedBean() {
        propagateNullOnNested(q -> q.map(BeanMapper.of(Test1Bean.class)));
    }

    public static class Test1Bean implements TestBean {

        private NestedBean1 nestedBean1;

        @Override
        public NestedBean1 getNestedBean() {
            return nestedBean1;
        }

        @Nested
        public void setNestedBean(NestedBean1 nestedBean1) {
            this.nestedBean1 = nestedBean1;
        }

        @PropagateNull("nestedid")
        public static class NestedBean1 implements TestBean.NestedBean {

            private String id;

            @Override
            public String getId() {
                return id;
            }

            @ColumnName("nestedid")
            public void setId(String id) {
                this.id = id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean does not use a prefix and the column in the nested bean is present in the
     * result set.
     */
    @Test
    void testPropagateNullOnNestedBeanWithFK() {
        propagateNullOnNestedWithFK(q -> q.map(BeanMapper.of(Test1FKBean.class)));
    }

    public static class Test1FKBean implements TestBean {

        private NestedBean1FK nestedBean1FK;

        @Override
        public NestedBean1FK getNestedBean() {
            return nestedBean1FK;
        }

        @Nested
        public void setNestedBean(NestedBean1FK nestedBean1FK) {
            this.nestedBean1FK = nestedBean1FK;
        }

        @PropagateNull("nestedfk")
        public static class NestedBean1FK implements TestBean.NestedBean {

            private String id;

            @Override
            public String getId() {
                return id;
            }

            @ColumnName("nestedid")
            public void setId(String id) {
                this.id = id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the snake_case mapper (parses prefixes)
     */
    @Test
    void testPropagateNullOnNestedBeanWithPrefixSnakeCase() {
        propagateNullOnNestedColumn(q -> q.map(BeanMapper.of(Test2Bean.class)));
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the case insensitive name mapper.
     */
    @Test
    void testPropagateNullOnNestedBeanWithPrefixCaseInsensitive() {
        testPropagateNullOnNestedWithPrefixCaseInsensitive(q -> q.map(BeanMapper.of(Test2Bean.class)));
    }

    public static class Test2Bean implements TestBean {

        private NestedBean2 nestedBean2;

        @Override
        public NestedBean2 getNestedBean() {
            return nestedBean2;
        }

        @Nested("bean")
        public void setNestedBean(NestedBean2 nestedBean2) {
            this.nestedBean2 = nestedBean2;
        }

        @PropagateNull("id")
        public static class NestedBean2 implements TestBean.NestedBean {

            private String id;

            @Override
            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the snake_case mapper (parses prefixes)
     */
    @Test
    void testPropagateNullOnNestedBeanWithPrefixSnakeCaseWithFK() {
        propagateNullOnNestedColumnWithFK(q -> q.map(BeanMapper.of(Test2FKBean.class)));
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the case insensitive name mapper.
     */
    @Test
    void testPropagateNullOnNestedBeanWithPrefixCaseInsensitiveWithFK() {
        testPropagateNullOnNestedWithPrefixCaseInsensitiveWithFK(q -> q.map(BeanMapper.of(Test2FKBean.class)));
    }

    public static class Test2FKBean implements TestBean {

        private NestedBean2FK nestedBean2FK;

        @Override
        public NestedBean2FK getNestedBean() {
            return nestedBean2FK;
        }

        @Nested("bean")
        public void setNestedBean(NestedBean2FK nestedBean2FK) {
            this.nestedBean2FK = nestedBean2FK;
        }

        @PropagateNull("fk")
        public static class NestedBean2FK implements TestBean.NestedBean {

            private String id;

            @Override
            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a a specific column in the nested bean works. The annotation is picked up from the column (not from the bean).
     */
    @Test
    void testPropagateNullOnNestedBeanColumn() {
        propagateNullOnNestedColumn(q -> q.map(BeanMapper.of(Test3Bean.class)));
    }

    public static class Test3Bean implements TestBean {

        private NestedBean3 nestedBean3;

        @Override
        public NestedBean3 getNestedBean() {
            return nestedBean3;
        }

        @Nested("bean")
        public void setNestedBean(NestedBean3 nestedBean3) {
            this.nestedBean3 = nestedBean3;
        }

        public static class NestedBean3 implements TestBean.NestedBean {

            private String id;

            @Override
            public String getId() {
                return id;
            }

            @PropagateNull
            public void setId(String id) {
                this.id = id;
            }
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Use an
     * explicit prefix for the nested bean ('n').
     */
    @Test
    void testDoubleNestedBeanPropagateNull() {
        doubleNestedPropagateNull(q -> q.map(BeanMapper.of(Test4Bean.class)));
    }

    public static class Test4Bean implements TestBean {

        private NestedBean4 nestedBean4;

        @Override
        public NestedBean4 getNestedBean() {
            return nestedBean4;
        }

        @Nested("n")
        @PropagateNull
        public void setNestedBean(NestedBean4 nestedBean4) {
            this.nestedBean4 = nestedBean4;
        }

        public static class NestedBean4 implements TestBean.NestedBean {

            private String id;

            @Override
            public String getId() {
                return id;
            }

            @PropagateNull
            public void setId(String id) {
                this.id = id;
            }
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Use an
     * explicit prefix for the nested bean ('n').
     */
    @Test
    void testDoubleNestedBeanPropagateNullWithFK() {
        doubleNestedPropagateNullWithFK(q -> q.map(BeanMapper.of(Test4FKBean.class)));
    }

    public static class Test4FKBean implements TestBean {

        private NestedBean4FK nestedBean4FK;

        @Override
        public NestedBean4FK getNestedBean() {
            return nestedBean4FK;
        }

        @Nested("n")
        @PropagateNull
        public void setNestedBean(NestedBean4FK nestedBean4FK) {
            this.nestedBean4FK = nestedBean4FK;
        }

        @PropagateNull("fk")
        public static class NestedBean4FK implements TestBean.NestedBean {

            private String id;

            @Override
            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Do not
     * actually set a prefix for the nested bean, so the properties are picked up without a prefix.
     */
    @Test
    void testDoubleNestedBeanPropagateNullWithImplicitPrefix() {
        doubleNestedPropagateNull(q -> q.map(BeanMapper.of(Test5Bean.class)));
    }

    public static class Test5Bean implements TestBean {

        private NestedBean5 nestedBean5;

        @Override
        public NestedBean5 getNestedBean() {
            return nestedBean5;
        }

        @Nested
        @PropagateNull
        public void setNestedBean(NestedBean5 nestedBean5) {
            this.nestedBean5 = nestedBean5;
        }

        public static class NestedBean5 implements TestBean.NestedBean {

            private String id;

            @Override
            public String getId() {
                return id;
            }

            @PropagateNull
            @ColumnName("nid")
            public void setId(String id) {
                this.id = id;
            }
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Do not
     * actually set a prefix for the nested bean, so the properties are picked up without a prefix.
     */
    @Test
    void testDoubleNestedBeanPropagateNullWithImplicitPrefixWithFK() {
        doubleNestedPropagateNullWithFK(q -> q.map(BeanMapper.of(Test5FKBean.class)));
    }

    public static class Test5FKBean implements TestBean {

        private NestedBean5FK nestedBean5FK;

        @Override
        public NestedBean5FK getNestedBean() {
            return nestedBean5FK;
        }

        @Nested
        @PropagateNull
        public void setNestedBean(NestedBean5FK nestedBean5FK) {
            this.nestedBean5FK = nestedBean5FK;
        }

        @PropagateNull("nfk")
        public static class NestedBean5FK implements TestBean.NestedBean {

            private String id;

            @Override
            public String getId() {
                return id;
            }

            @ColumnName("nid")
            public void setId(String id) {
                this.id = id;
            }
        }
    }

    @Test
    void testBadPropagateNullAnnotationOnBean() {
        assertThatThrownBy(() -> testPropagateNullOnNestedWithPrefixCaseInsensitive(q -> q.map(BeanMapper.of(Test6Bean.class))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@PropagateNull does not support a value (id)")
                .hasMessageContaining("(nestedBean)");
    }

    public static class Test6Bean implements TestBean {

        private NestedBean6 nestedBean6;

        @Nested("bean")
        @PropagateNull("id")
        public void setNestedBean(NestedBean6 nestedBean6) {
            this.nestedBean6 = nestedBean6;
        }

        @Override
        public NestedBean6 getNestedBean() {
            return nestedBean6;
        }

        public static class NestedBean6 implements TestBean.NestedBean {

            private String id;

            public void setId(String id) {
                this.id = id;
            }

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean does not use a prefix and the column in the nested bean is present in the
     * result set.
     */
    @Test
    void testPropagateNullOnNestedCtor() {
        propagateNullOnNested(q -> q.map(ConstructorMapper.of(Test11Bean.class)));
    }

    public static class Test11Bean implements TestBean {

        private final NestedBean11 nestedBean11;

        public Test11Bean(@Nested NestedBean11 nestedBean11) {
            this.nestedBean11 = nestedBean11;
        }

        @Override
        public NestedBean11 getNestedBean() {
            return nestedBean11;
        }

        @PropagateNull("nestedid")
        public static class NestedBean11 implements TestBean.NestedBean {

            private final String id;

            public NestedBean11(@ColumnName("nestedid") String id) {
                this.id = id;
            }

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean does not use a prefix and the column in the nested bean is present in the
     * result set.
     */
    @Test
    void testPropagateNullOnNestedCtorWithFK() {
        propagateNullOnNestedWithFK(q -> q.map(ConstructorMapper.of(Test11FKBean.class)));
    }

    public static class Test11FKBean implements TestBean {

        private final NestedBean11FK nestedBean11FK;

        public Test11FKBean(@Nested NestedBean11FK nestedBean11FK) {
            this.nestedBean11FK = nestedBean11FK;
        }

        @Override
        public NestedBean11FK getNestedBean() {
            return nestedBean11FK;
        }

        @PropagateNull("nestedfk")
        public static class NestedBean11FK implements TestBean.NestedBean {

            private final String id;

            public NestedBean11FK(@ColumnName("nestedid") String id) {
                this.id = id;
            }

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the snake_case mapper (parses prefixes)
     */
    @Test
    void testPropagateNullOnNestedCtorWithPrefixSnakeCase() {
        propagateNullOnNestedColumn(q -> q.map(ConstructorMapper.of(Test12Bean.class)));
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the case insensitive name mapper.
     */
    @Test
    void testPropagateNullOnNestedCtorWithPrefixCaseInsensitive() {
        testPropagateNullOnNestedWithPrefixCaseInsensitive(q -> q.map(ConstructorMapper.of(Test12Bean.class)));
    }

    public static class Test12Bean implements TestBean {

        private final NestedBean12 nestedBean12;

        public Test12Bean(@Nested("bean") NestedBean12 nestedBean12) {
            this.nestedBean12 = nestedBean12;
        }

        @Override
        public NestedBean12 getNestedBean() {
            return nestedBean12;
        }

        @PropagateNull("id")
        public static class NestedBean12 implements TestBean.NestedBean {

            private final String id;

            public NestedBean12(String id) {
                this.id = id;
            }

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the snake_case mapper (parses prefixes)
     */
    @Test
    void testPropagateNullOnNestedCtorWithPrefixSnakeCaseWithFK() {
        propagateNullOnNestedColumnWithFK(q -> q.map(ConstructorMapper.of(Test12FKBean.class)));
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the case insensitive name mapper.
     */
    @Test
    void testPropagateNullOnNestedCtorWithPrefixCaseInsensitiveWithFK() {
        testPropagateNullOnNestedWithPrefixCaseInsensitiveWithFK(q -> q.map(ConstructorMapper.of(Test12FKBean.class)));
    }

    public static class Test12FKBean implements TestBean {

        private final NestedBean12FK nestedBean12FK;

        public Test12FKBean(@Nested("bean") NestedBean12FK nestedBean12FK) {
            this.nestedBean12FK = nestedBean12FK;
        }

        @Override
        public NestedBean12FK getNestedBean() {
            return nestedBean12FK;
        }

        @PropagateNull("fk")
        public static class NestedBean12FK implements TestBean.NestedBean {

            private final String id;

            public NestedBean12FK(String id) {
                this.id = id;
            }

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a a specific column in the nested bean works. The annotation is picked up from the column (not from the bean).
     */
    @Test
    void testPropagateNullOnNestedCtorColumn() {
        propagateNullOnNestedColumn(q -> q.map(ConstructorMapper.of(Test13Bean.class)));
    }

    public static class Test13Bean implements TestBean {

        private final NestedBean13 nestedBean13;

        public Test13Bean(@Nested("bean") NestedBean13 nestedBean13) {
            this.nestedBean13 = nestedBean13;
        }

        @Override
        public NestedBean13 getNestedBean() {
            return nestedBean13;
        }

        public static class NestedBean13 implements TestBean.NestedBean {

            private final String id;

            public NestedBean13(@PropagateNull String id) {
                this.id = id;
            }

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Use an
     * explicit prefix for the nested bean ('n').
     */
    @Test
    void testDoubleNestedCtorPropagateNull() {
        doubleNestedPropagateNull(q -> q.map(ConstructorMapper.of(Test14Bean.class)));
    }

    public static class Test14Bean implements TestBean {

        private final NestedBean14 nestedBean14;

        public Test14Bean(@Nested("n") @PropagateNull NestedBean14 nestedBean14) {
            this.nestedBean14 = nestedBean14;
        }

        @Override
        public NestedBean14 getNestedBean() {
            return nestedBean14;
        }

        public static class NestedBean14 implements TestBean.NestedBean {

            private final String id;

            public NestedBean14(@PropagateNull String id) {
                this.id = id;
            }

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Use an
     * explicit prefix for the nested bean ('n').
     */
    @Test
    void testDoubleNestedCtorPropagateNullWithFK() {
        doubleNestedPropagateNullWithFK(q -> q.map(ConstructorMapper.of(Test14FKBean.class)));
    }

    public static class Test14FKBean implements TestBean {

        private final NestedBean14FK nestedBean14FK;

        public Test14FKBean(@Nested("n") @PropagateNull NestedBean14FK nestedBean14FK) {
            this.nestedBean14FK = nestedBean14FK;
        }

        @Override
        public NestedBean14FK getNestedBean() {
            return nestedBean14FK;
        }

        @PropagateNull("fk")
        public static class NestedBean14FK implements TestBean.NestedBean {

            private final String id;

            public NestedBean14FK(String id) {
                this.id = id;
            }

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Do not
     * actually set a prefix for the nested bean, so the properties are picked up without a prefix.
     */
    @Test
    void testDoubleNestedPropagateNullCtorWithImplicitPrefix() {
        doubleNestedPropagateNull(q -> q.map(ConstructorMapper.of(Test15Bean.class)));
    }

    public static class Test15Bean implements TestBean {

        private final NestedBean15 nestedBean15;

        public Test15Bean(@Nested @PropagateNull NestedBean15 nestedBean15) {
            this.nestedBean15 = nestedBean15;
        }

        @Override
        public NestedBean15 getNestedBean() {
            return nestedBean15;
        }

        public static class NestedBean15 implements TestBean.NestedBean {

            private final String id;

            public NestedBean15(@PropagateNull @ColumnName("nid") String id) {
                this.id = id;
            }

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Do not
     * actually set a prefix for the nested bean, so the properties are picked up without a prefix.
     */
    @Test
    void testDoubleNestedPropagateNullCtorWithImplicitPrefixWithFK() {
        doubleNestedPropagateNullWithFK(q -> q.map(ConstructorMapper.of(Test15FKBean.class)));
    }

    public static class Test15FKBean implements TestBean {

        private final NestedBean15FK nestedBean15FK;

        public Test15FKBean(@Nested @PropagateNull NestedBean15FK nestedBean15FK) {
            this.nestedBean15FK = nestedBean15FK;
        }

        @Override
        public NestedBean15FK getNestedBean() {
            return nestedBean15FK;
        }

        @PropagateNull("nfk")
        public static class NestedBean15FK implements TestBean.NestedBean {

            private final String id;

            public NestedBean15FK(@ColumnName("nid") String id) {
                this.id = id;
            }

            @Override
            public String getId() {
                return id;
            }
        }
    }

    @Test
    void testBadPropagateNullAnnotationOnCtor() {
        assertThatThrownBy(() -> testPropagateNullOnNestedWithPrefixCaseInsensitive(q -> q.map(ConstructorMapper.of(Test16Bean.class))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@PropagateNull does not support a value (id)")
                .hasMessageContaining("(nestedBean16)");
    }

    public static class Test16Bean implements TestBean {

        private final NestedBean16 nestedBean16;

        public Test16Bean(@Nested("bean") @PropagateNull("id") NestedBean16 nestedBean16) {
            this.nestedBean16 = nestedBean16;
        }

        @Override
        public NestedBean16 getNestedBean() {
            return nestedBean16;
        }

        public static class NestedBean16 implements TestBean.NestedBean {

            private final String id;

            public NestedBean16(String id) {
                this.id = id;
            }

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean does not use a prefix and the column in the nested bean is present in the
     * result set.
     */
    @Test
    void testPropagateNullOnNestedField() {
        propagateNullOnNested(q -> q.map(FieldMapper.of(Test21Bean.class)));
    }

    public static class Test21Bean implements TestBean {

        @Nested
        public NestedBean21 nestedBean21;

        @Override
        public NestedBean21 getNestedBean() {
            return nestedBean21;
        }

        @PropagateNull("nestedid")
        public static class NestedBean21 implements TestBean.NestedBean {

            @ColumnName("nestedid")
            private String id;

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean does not use a prefix and the column in the nested bean is present in the
     * result set.
     */
    @Test
    void testPropagateNullOnNestedFieldWithFK() {
        propagateNullOnNestedWithFK(q -> q.map(FieldMapper.of(Test21FKBean.class)));
    }

    public static class Test21FKBean implements TestBean {

        @Nested
        public NestedBean21FK nestedBean21FK;

        @Override
        public NestedBean21FK getNestedBean() {
            return nestedBean21FK;
        }

        @PropagateNull("nestedfk")
        public static class NestedBean21FK implements TestBean.NestedBean {

            @ColumnName("nestedid")
            private String id;

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the snake_case mapper (parses prefixes)
     */
    @Test
    void testPropagateNullOnNestedFieldWithPrefixSnakeCase() {
        propagateNullOnNestedColumn(q -> q.map(FieldMapper.of(Test22Bean.class)));
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the case insensitive name mapper.
     */
    @Test
    void testPropagateNullOnNestedFieldWithPrefixCaseInsensitive() {
        testPropagateNullOnNestedWithPrefixCaseInsensitive(q -> q.map(FieldMapper.of(Test22Bean.class)));
    }

    public static class Test22Bean implements TestBean {

        @Nested("bean")
        public NestedBean22 nestedBean22;

        @Override
        public NestedBean22 getNestedBean() {
            return nestedBean22;
        }

        @PropagateNull("id")
        public static class NestedBean22 implements TestBean.NestedBean {

            public String id;

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the snake_case mapper (parses prefixes)
     */
    @Test
    void testPropagateNullOnNestedFieldWithPrefixSnakeCaseWithFK() {
        propagateNullOnNestedColumnWithFK(q -> q.map(FieldMapper.of(Test22FKBean.class)));
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the case insensitive name mapper.
     */
    @Test
    void testPropagateNullOnNestedFieldWithPrefixCaseInsensitiveWithFK() {
        testPropagateNullOnNestedWithPrefixCaseInsensitiveWithFK(q -> q.map(FieldMapper.of(Test22FKBean.class)));
    }

    public static class Test22FKBean implements TestBean {

        @Nested("bean")
        public NestedBean22FK nestedBean22FK;

        @Override
        public NestedBean22FK getNestedBean() {
            return nestedBean22FK;
        }

        @PropagateNull("fk")
        public static class NestedBean22FK implements TestBean.NestedBean {

            public String id;

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that the propagateNull annotation on a a specific column in the nested bean works. The annotation is picked up from the column (not from the bean).
     */
    @Test
    void testPropagateNullOnNestedFieldColumn() {
        propagateNullOnNestedColumn(q -> q.map(FieldMapper.of(Test23Bean.class)));
    }

    public static class Test23Bean implements TestBean {

        @Nested("bean")
        public NestedBean23 nestedBean23;

        @Override
        public NestedBean23 getNestedBean() {
            return nestedBean23;
        }

        public static class NestedBean23 implements TestBean.NestedBean {

            @PropagateNull
            public String id;

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Use an
     * explicit prefix for the nested bean ('n').
     */
    @Test
    void testDoubleNestedFieldPropagateNull() {
        doubleNestedPropagateNull(q -> q.map(FieldMapper.of(Test24Bean.class)));
    }

    public static class Test24Bean implements TestBean {

        @Nested("n")
        @PropagateNull
        public NestedBean24 nestedBean24;

        @Override
        public NestedBean24 getNestedBean() {
            return nestedBean24;
        }

        public static class NestedBean24 implements TestBean.NestedBean {

            @PropagateNull
            public String id;

            @Override
            public String getId() {
                return id;
            }

        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Use an
     * explicit prefix for the nested bean ('n').
     */
    @Test
    void testDoubleNestedFieldPropagateNullWithFK() {
        doubleNestedPropagateNullWithFK(q -> q.map(FieldMapper.of(Test24FKBean.class)));
    }

    public static class Test24FKBean implements TestBean {

        @Nested("n")
        @PropagateNull
        public NestedBean24FK nestedBean24FK;

        @Override
        public NestedBean24FK getNestedBean() {
            return nestedBean24FK;
        }

        @PropagateNull("fk")
        public static class NestedBean24FK implements TestBean.NestedBean {

            public String id;

            @Override
            public String getId() {
                return id;
            }

        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Do not
     * actually set a prefix for the nested bean, so the properties are picked up without a prefix.
     */
    @Test
    void testDoubleNestedFieldPropagateNullWithImplicitPrefix() {
        doubleNestedPropagateNull(q -> q.map(FieldMapper.of(Test25Bean.class)));
    }

    public static class Test25Bean implements TestBean {

        @Nested
        @PropagateNull
        public NestedBean25 nestedBean25;

        @Override
        public NestedBean25 getNestedBean() {
            return nestedBean25;
        }

        public static class NestedBean25 implements TestBean.NestedBean {

            @PropagateNull
            @ColumnName("nid")
            public String id;

            @Override
            public String getId() {
                return id;
            }
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well. Do not
     * actually set a prefix for the nested bean, so the properties are picked up without a prefix.
     */
    @Test
    void testDoubleNestedFieldPropagateNullWithImplicitPrefixWithFK() {
        doubleNestedPropagateNullWithFK(q -> q.map(FieldMapper.of(Test25FKBean.class)));
    }

    public static class Test25FKBean implements TestBean {

        @Nested
        @PropagateNull
        public NestedBean25FK nestedBean25FK;

        @Override
        public NestedBean25FK getNestedBean() {
            return nestedBean25FK;
        }

        @PropagateNull("nfk")
        public static class NestedBean25FK implements TestBean.NestedBean {

            @ColumnName("nid")
            public String id;

            @Override
            public String getId() {
                return id;
            }
        }
    }

    @Test
    void testBadPropagateNullAnnotationOnField() {
        assertThatThrownBy(() -> testPropagateNullOnNestedWithPrefixCaseInsensitive(q -> q.map(FieldMapper.of(Test26Bean.class))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@PropagateNull does not support a value (id)")
                .hasMessageContaining("(nestedBean26)");
    }

    public static class Test26Bean implements TestBean {

        @Nested("bean")
        @PropagateNull("id")
        public NestedBean26 nestedBean26;

        @Override
        public NestedBean26 getNestedBean() {
            return nestedBean26;
        }

        public static class NestedBean26 implements TestBean.NestedBean {

            public String id;

            @Override
            public String getId() {
                return id;
            }
        }
    }
}
