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
package org.jdbi.v3.core.test.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.test.junit5.H2DatabaseExtension.SOMETHING_INITIALIZER;

import java.util.function.Function;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.test.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class AbstractPropagateNullTest {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withPlugin(getPlugin()).withInitializer(SOMETHING_INITIALIZER);

    protected Handle handle;

    protected JdbiPlugin getPlugin() {
        return new JdbiPlugin() {};
    }

    @BeforeEach
    void setUp() {
        this.handle = h2Extension.getSharedHandle();
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean does not use a prefix and the column in the nested bean is present in the
     * result set.
     */
    protected void propagateNullOnNested(Function<Query, ResultIterable<? extends TestBean>> mapFunction) {
        try (Query select = handle.select("select 'fourty-two' as nestedid")) {
            TestBean testBean = mapFunction.apply(select).one();

            assertThat(testBean).isNotNull();
            assertThat(testBean.getNestedBean()).isNotNull();
            assertThat(testBean.getNestedBean().getId()).isEqualTo("fourty-two");

            testBean = mapFunction.apply(handle.select("select NULL as nestedid"))
                .one();

            assertThat(testBean).isNotNull();
            assertThat(testBean.getNestedBean()).isNull();
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the case insensitive name mapper.
     */
    protected void testPropagateNullOnNestedWithPrefixCaseInsensitive(Function<Query, ResultIterable<? extends TestBean>> mapFunction) {

        try (Query select = handle.select("select 'fourty-two' as beanID")) {
            // use the case-insensitive column name mapper
            TestBean testBean = mapFunction.apply(select).one();

            assertThat(testBean).isNotNull();
            assertThat(testBean.getNestedBean()).isNotNull();
            assertThat(testBean.getNestedBean().getId()).isEqualTo("fourty-two");

            testBean = mapFunction.apply(handle.select("select NULL as beanID"))
                .one();

            assertThat(testBean).isNotNull()
                .extracting(TestBean::getNestedBean).isNull();
        }
    }

    /**
     * Test that the propagateNull annotation on a a specific column in the nested bean works. The annotation is picked up from the column (not from the bean).
     */
    protected void propagateNullOnNestedColumn(Function<Query, ResultIterable<? extends TestBean>> mapFunction) {

        try (Query select = handle.select("select 'fourty-two' as bean_id")) {
            // use the snake case column name mapper
            TestBean testBean = mapFunction.apply(select).one();

            assertThat(testBean).isNotNull();
            assertThat(testBean.getNestedBean()).isNotNull();
            assertThat(testBean.getNestedBean().getId()).isEqualTo("fourty-two");

            testBean = mapFunction.apply(handle.select("select NULL as bean_id")).one();

            assertThat(testBean).isNotNull()
                .extracting(TestBean::getNestedBean).isNull();
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well.
     */
    protected void doubleNestedPropagateNull(Function<Query, ResultIterable<? extends TestBean>> mapFunction) {

        try (Query select = handle.select("select 'fourty-two' as nid")) {
            TestBean testBean = mapFunction.apply(select)
                .one();

            assertThat(testBean).isNotNull();
            assertThat(testBean.getNestedBean()).isNotNull();
            assertThat(testBean.getNestedBean().getId()).isEqualTo("fourty-two");

            testBean = mapFunction.apply(handle.select("select NULL as nid")).one();

            assertThat(testBean).isNull();
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean does not use a prefix and the column in the nested bean is present in the
     * result set. The tested key is different from the id key (and may not be mapped by the bean).
     */
    protected void propagateNullOnNestedWithFK(Function<Query, ResultIterable<? extends TestBean>> mapFunction) {
        try (Query select = handle.select("select 'fourty-two' as nestedid, 1 as nestedfk")) {
            TestBean testBean = mapFunction.apply(select).one();

            assertThat(testBean).isNotNull();
            assertThat(testBean.getNestedBean()).isNotNull();
            assertThat(testBean.getNestedBean().getId()).isEqualTo("fourty-two");

            testBean = mapFunction.apply(handle.select("select 'fourty-two' as nestedid, NULL as nestedfk"))
                .one();

            assertThat(testBean).isNotNull()
                .extracting(TestBean::getNestedBean).isNull();
        }
    }

    /**
     * Test that the propagateNull annotation on a nested bean works. The nested bean has a prefix and the column in the nested bean has no special name. The
     * value in the result set is prefixed using the bean prefix. Use the case insensitive name mapper.
     */
    protected void testPropagateNullOnNestedWithPrefixCaseInsensitiveWithFK(Function<Query, ResultIterable<? extends TestBean>> mapFunction) {

        try (Query select = handle.select("select 'fourty-two' as beanID, 1 as beanFK")) {
            // use the case-insensitive column name mapper
            TestBean testBean = mapFunction.apply(select).one();

            assertThat(testBean).isNotNull();
            assertThat(testBean.getNestedBean()).isNotNull();
            assertThat(testBean.getNestedBean().getId()).isEqualTo("fourty-two");

            testBean = mapFunction.apply(handle.select("select 'fourty-two' as beanID, NULL as beanFK"))
                .one();

            assertThat(testBean).isNotNull()
                .extracting(TestBean::getNestedBean).isNull();
        }
    }

    /**
     * Test that the propagateNull annotation on a a specific column in the nested bean works. The annotation is picked up from the column (not from the bean).
     */
    protected void propagateNullOnNestedColumnWithFK(Function<Query, ResultIterable<? extends TestBean>> mapFunction) {

        try (Query select = handle.select("select 'fourty-two' as bean_id, 1 as bean_fk")) {
            // use the snake case column name mapper
            TestBean testBean = mapFunction.apply(select).one();

            assertThat(testBean).isNotNull();
            assertThat(testBean.getNestedBean()).isNotNull();
            assertThat(testBean.getNestedBean().getId()).isEqualTo("fourty-two");

            testBean = mapFunction.apply(handle.select("select 'fourty-two' as bean_id, NULL as bean_fk"))
                .one();

            assertThat(testBean).isNotNull()
                .extracting(TestBean::getNestedBean).isNull();
        }
    }

    /**
     * Test that propagateNull is bubbled up. If the property in the nested bean is null, not only null out the nested bean but the prent bean as well.
     */
    protected void doubleNestedPropagateNullWithFK(Function<Query, ResultIterable<? extends TestBean>> mapFunction) {

        try (Query select = handle.select("select 'fourty-two' as nid, 1 as nfk")) {
            TestBean testBean = mapFunction.apply(select).one();

            assertThat(testBean).isNotNull();
            assertThat(testBean.getNestedBean()).isNotNull();
            assertThat(testBean.getNestedBean().getId()).isEqualTo("fourty-two");

            testBean = mapFunction.apply(handle.select("select 'fourty-two' as nid, NULL as nfk")).one();

            assertThat(testBean).isNull();
        }
    }

    public interface TestBean {

        NestedBean getNestedBean();

        interface NestedBean {
            String getId();
        }
    }
}
