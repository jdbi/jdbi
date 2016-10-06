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
package org.jdbi.v3.core.util.bean;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SnakeCaseColumnNameStrategyTest {

    final SnakeCaseColumnNameStrategy snake = new SnakeCaseColumnNameStrategy();

    @Test
    public void testSimple() {
        assertThat(snake.nameMatches("testPropertyWithCheese", "test_property_with_cheese")).isTrue();
    }

    @Test
    public void testWithoutCheese() {
        assertThat(snake.nameMatches("testPropertyWithoutCheese", "test_property_with_cheese")).isFalse();
    }

    @Test
    public void testBeanNumbers() {
        assertThat(snake.nameMatches("testProperty2", "test_property_2")).isTrue();
    }

    @Test
    public void testNumbersDontMatch() {
        assertThat(snake.nameMatches("testProperty3", "test_property_2")).isFalse();
    }
}
