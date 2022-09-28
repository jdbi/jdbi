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
package org.jdbi.v3.core.mapper.reflect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaseInsensitiveColumnNameMatcherTest {

    final ColumnNameMatcher matcher = new CaseInsensitiveColumnNameMatcher();

    @Test
    public void testSimple() {
        assertThat(matcher.columnNameMatches("foobar", "fooBar")).isTrue();
    }

    @Test
    public void testColumnWithSeparator() {
        assertThat(matcher.columnNameMatches("test_property", "testProperty")).isFalse();
    }

    @Test
    public void testNameWithSinglePrefix() {
        assertThat(matcher.columnNameMatches("foobarbaz", "foo.barBaz")).isTrue();
    }

    @Test
    public void testNameWithMultiplePrefix() {
        assertThat(matcher.columnNameMatches("foobarbaz", "foo.bar.baz")).isTrue();
    }

    @Test
    public void testNameDoesNotMatchWithoutPrefix() {
        assertThat(matcher.columnNameMatches("foo_barbaz", "barBaz")).isFalse();
    }

    @Test
    public void testIgnoreShortProperties() {
        assertThat(matcher.columnNameMatches("foobarbaz", "foo.bar")).isFalse();
    }

    @Test
    public void testIgnoreShortNames() {
        assertThat(matcher.columnNameMatches("foobar", "foo.bar.baz")).isFalse();
    }

    @Test
    public void testIgnorePrefixOnly() {
        assertThat(matcher.columnNameMatches("foobar", "foo")).isFalse();
    }

    @Test
    public void testWithWeirdPrefix() {
        assertThat(matcher.columnNameMatches("foo_bar", "foo_.bar")).isTrue();
    }

    @Test
    public void testWithNoSeparatorPrefix() {
        assertThat(matcher.columnNameMatches("foobar", "foo.bar")).isTrue();
    }

    @Test
    public void testPrefixSimple() {
        assertThat(matcher.columnNameStartsWith("testpropertywithcheese", "testPropertyWith")).isTrue();
    }

    @Test
    public void testPrefixWithoutCheese() {
        assertThat(matcher.columnNameStartsWith("testpropertywithcheese", "testPropertyWithout")).isFalse();
    }

    @Test
    public void testPrefixNameWithSinglePrefix() {
        assertThat(matcher.columnNameStartsWith("foobarbaz", "foo.bar")).isTrue();
    }

    @Test
    public void testPrefixNameWithMultiplePrefix() {
        assertThat(matcher.columnNameStartsWith("foobarbaz", "foo.bar")).isTrue();
    }

    @Test
    public void testPrefixNameDoesNotMatchWithoutPrefix() {
        assertThat(matcher.columnNameStartsWith("foobarbaz", "bar")).isFalse();
    }

    @Test
    public void testPrefixIgnoreShortProperties() {
        assertThat(matcher.columnNameStartsWith("foobarbaz", "foo")).isTrue();
    }

    @Test
    public void testPrefixWithWeirdPrefix() {
        assertThat(matcher.columnNameStartsWith("foo_bar", "foo_")).isTrue();
    }

    @Test
    public void testPrefixWithNoSeparatorPrefix() {
        assertThat(matcher.columnNameStartsWith("foobar", "foo")).isTrue();
    }
}
