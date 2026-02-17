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
package org.jdbi.core.mapper.reflect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SnakeCaseColumnNameMatcherTest {

    final SnakeCaseColumnNameMatcher snake = new SnakeCaseColumnNameMatcher();

    @Test
    public void testSimple() {
        assertThat(snake.columnNameMatches("test_property_with_cheese", "testPropertyWithCheese")).isTrue();
    }

    @Test
    public void testWithoutCheese() {
        assertThat(snake.columnNameMatches("test_property_with_cheese", "testPropertyWithoutCheese")).isFalse();
    }

    @Test
    public void testBeanNumbers() {
        assertThat(snake.columnNameMatches("test_property_2", "testProperty2")).isTrue();
    }

    @Test
    public void testNumbersDontMatch() {
        assertThat(snake.columnNameMatches("test_property_2", "testProperty3")).isFalse();
    }

    @Test
    public void testNameWithoutPrefix() {
        assertThat(snake.columnNameMatches("foo_bar_baz", "fooBarBaz")).isTrue();
    }

    @Test
    public void testNameWithSinglePrefix() {
        assertThat(snake.columnNameMatches("foo_bar_baz", "foo.barBaz")).isTrue();
    }

    @Test
    public void testNameWithMultiplePrefix() {
        assertThat(snake.columnNameMatches("foo_bar_baz", "foo.bar.baz")).isTrue();
    }

    @Test
    public void testNameDoesNotMatchWithoutPrefix() {
        assertThat(snake.columnNameMatches("foo_bar_baz", "barBaz")).isFalse();
    }

    @Test
    public void testIgnoreShortProperties() {
        assertThat(snake.columnNameMatches("foo_bar_baz", "foo.bar")).isFalse();
    }

    @Test
    public void testIgnoreShortNames() {
        assertThat(snake.columnNameMatches("foo_bar", "foo.bar.baz")).isFalse();
    }

    @Test
    public void testIgnorePrefixOnly() {
        assertThat(snake.columnNameMatches("foo_bar", "foo")).isFalse();
    }

    @Test
    public void testWithWeirdPrefix() {
        assertThat(snake.columnNameMatches("foo_bar", "foo_.bar")).isTrue();
    }

    @Test
    public void testPrefixSimple() {
        assertThat(snake.columnNameStartsWith("test_property_with_cheese", "testPropertyWith")).isTrue();
    }

    @Test
    public void testPrefixWithoutCheese() {
        assertThat(snake.columnNameStartsWith("test_property_with_cheese", "testPropertyWithout")).isFalse();
    }

    @Test
    public void testPrefixBeanNumbers() {
        assertThat(snake.columnNameStartsWith("test_property_2", "testProperty")).isTrue();
    }

    @Test
    public void testPrefixNameWithoutPrefix() {
        assertThat(snake.columnNameStartsWith("foo_bar_baz", "fooBar")).isTrue();
    }

    @Test
    public void testPrefixNameWithSinglePrefix() {
        assertThat(snake.columnNameStartsWith("foo_bar_baz", "foo.bar")).isTrue();
    }

    @Test
    public void testPrefixNameWithMultiplePrefix() {
        assertThat(snake.columnNameStartsWith("foo_bar_baz", "foo.bar")).isTrue();
    }

    @Test
    public void testPrefixNameDoesNotMatchWithoutPrefix() {
        assertThat(snake.columnNameStartsWith("foo_bar_baz", "bar")).isFalse();
    }

    @Test
    public void testPrefixIgnoreShortProperties() {
        assertThat(snake.columnNameStartsWith("foo_bar_baz", "foo")).isTrue();
    }

    @Test
    public void testPrefixWithWeirdPrefix() {
        assertThat(snake.columnNameStartsWith("foo_bar", "foo_")).isTrue();
    }

    @Test
    public void testPrefixWithNoSeparatorPrefix() {
        assertThat(snake.columnNameStartsWith("foobar", "foo")).isTrue();
    }

}
