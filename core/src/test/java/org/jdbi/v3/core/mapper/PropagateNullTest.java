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
package org.jdbi.v3.core.mapper;

import org.jdbi.v3.core.mapper.reflect.BeanMapper;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.mapper.reflect.FieldMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        private NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @Nested
        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @PropagateNull("nestedid")
        public static class NestedBean implements TestBean.NestedBean {

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

        private NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @Nested
        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @PropagateNull("nestedfk")
        public static class NestedBean implements TestBean.NestedBean {

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

        private NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @Nested("bean")
        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @PropagateNull("id")
        public static class NestedBean implements TestBean.NestedBean {

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

        private NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @Nested("bean")
        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @PropagateNull("fk")
        public static class NestedBean implements TestBean.NestedBean {

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

        private NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @Nested("bean")
        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        public static class NestedBean implements TestBean.NestedBean {

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

        private NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @Nested("n")
        @PropagateNull
        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        public static class NestedBean implements TestBean.NestedBean {

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

        private NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @Nested("n")
        @PropagateNull
        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @PropagateNull("fk")
        public static class NestedBean implements TestBean.NestedBean {

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

        private NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @Nested
        @PropagateNull
        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        public static class NestedBean implements TestBean.NestedBean {

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

        private NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @Nested
        @PropagateNull
        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @PropagateNull("nfk")
        public static class NestedBean implements TestBean.NestedBean {

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
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> testPropagateNullOnNestedWithPrefixCaseInsensitive(q -> q.map(BeanMapper.of(Test6Bean.class))));
        assertThat(e.getMessage()).containsIgnoringCase("@PropagateNull does not support a value (id)");
        assertThat(e.getMessage()).containsIgnoringCase("(nestedBean)");
    }

    public static class Test6Bean implements TestBean {

        private NestedBean nestedBean;

        @Nested("bean")
        @PropagateNull("id")
        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public static class NestedBean implements TestBean.NestedBean {

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

        private final NestedBean nestedBean;

        public Test11Bean(@Nested NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @PropagateNull("nestedid")
        public static class NestedBean implements TestBean.NestedBean {

            private final String id;

            public NestedBean(@ColumnName("nestedid") String id) {
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

        private final NestedBean nestedBean;

        public Test11FKBean(@Nested NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @PropagateNull("nestedfk")
        public static class NestedBean implements TestBean.NestedBean {

            private final String id;

            public NestedBean(@ColumnName("nestedid") String id) {
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

        private final NestedBean nestedBean;

        public Test12Bean(@Nested("bean") NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @PropagateNull("id")
        public static class NestedBean implements TestBean.NestedBean {

            private final String id;

            public NestedBean(String id) {
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

        private final NestedBean nestedBean;

        public Test12FKBean(@Nested("bean") NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @PropagateNull("fk")
        public static class NestedBean implements TestBean.NestedBean {

            private final String id;

            public NestedBean(String id) {
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

        private final NestedBean nestedBean;

        public Test13Bean(@Nested("bean") NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public static class NestedBean implements TestBean.NestedBean {

            private final String id;

            public NestedBean(@PropagateNull String id) {
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

        private final NestedBean nestedBean;

        public Test14Bean(@Nested("n") @PropagateNull NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public static class NestedBean implements TestBean.NestedBean {

            private final String id;

            public NestedBean(@PropagateNull String id) {
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

        private final NestedBean nestedBean;

        public Test14FKBean(@Nested("n") @PropagateNull NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @PropagateNull("fk")
        public static class NestedBean implements TestBean.NestedBean {

            private final String id;

            public NestedBean(String id) {
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

        private final NestedBean nestedBean;

        public Test15Bean(@Nested @PropagateNull NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public static class NestedBean implements TestBean.NestedBean {

            private final String id;

            public NestedBean(@PropagateNull @ColumnName("nid") String id) {
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

        private final NestedBean nestedBean;

        public Test15FKBean(@Nested @PropagateNull NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @PropagateNull("nfk")
        public static class NestedBean implements TestBean.NestedBean {

            private final String id;

            public NestedBean(@ColumnName("nid") String id) {
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
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> testPropagateNullOnNestedWithPrefixCaseInsensitive(q -> q.map(ConstructorMapper.of(Test16Bean.class))));
        assertThat(e.getMessage()).containsIgnoringCase("@PropagateNull does not support a value (id)");
        assertThat(e.getMessage()).containsIgnoringCase("(nestedBean)");
    }

    public static class Test16Bean implements TestBean {

        private final NestedBean nestedBean;

        public Test16Bean(@Nested("bean") @PropagateNull("id") NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public static class NestedBean implements TestBean.NestedBean {

            private final String id;

            public NestedBean(String id) {
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
        public NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @PropagateNull("nestedid")
        public static class NestedBean implements TestBean.NestedBean {

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
        public NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @PropagateNull("nestedfk")
        public static class NestedBean implements TestBean.NestedBean {

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
        public NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @PropagateNull("id")
        public static class NestedBean implements TestBean.NestedBean {

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
        public NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @PropagateNull("fk")
        public static class NestedBean implements TestBean.NestedBean {

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
        public NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public static class NestedBean implements TestBean.NestedBean {

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
        public NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public static class NestedBean implements TestBean.NestedBean {

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
        public NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @PropagateNull("fk")
        public static class NestedBean implements TestBean.NestedBean {

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
        public NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public static class NestedBean implements TestBean.NestedBean {

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
        public NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        @PropagateNull("nfk")
        public static class NestedBean implements TestBean.NestedBean {

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
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> testPropagateNullOnNestedWithPrefixCaseInsensitive(q -> q.map(FieldMapper.of(Test26Bean.class))));
        assertThat(e.getMessage()).containsIgnoringCase("@PropagateNull does not support a value (id)");
        assertThat(e.getMessage()).containsIgnoringCase("(nestedBean)");
    }

    public static class Test26Bean implements TestBean {

        @Nested("bean")
        @PropagateNull("id")
        public NestedBean nestedBean;

        @Override
        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public static class NestedBean implements TestBean.NestedBean {

            public String id;

            @Override
            public String getId() {
                return id;
            }
        }
    }
}
