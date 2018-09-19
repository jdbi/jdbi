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
package org.jdbi.v3.core.locator.internal;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestClasspathBuilder {
    private final ClasspathBuilder builder = new ClasspathBuilder();

    @Test
    public void testEmpty() {
        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testWhitespace() {
        assertThat(builder.appendVerbatim("// abc // / xyz / /").build())
            .isEqualTo("abc/xyz");
    }

    @Test
    public void testSimpleVerbatimWithExtension() {
        assertThat(builder.appendVerbatim("foo").appendVerbatim("bar").setExtension("baz").build())
            .isEqualTo("foo/bar.baz");
    }

    @Test
    public void testWeirdVerbatim() {
        assertThat(builder.appendVerbatim("foo.bar@x-y-z").build())
            .isEqualTo("foo.bar@x-y-z");
    }

    @Test
    public void testDotPath() {
        assertThat(builder.appendDotPath("foo.bar.baz").build())
            .isEqualTo("foo/bar/baz");
    }

    @Test
    public void testFqClass() {
        assertThat(builder.appendFullyQualifiedClassName(String.class).build())
            .isEqualTo("java/lang/String");
    }

    @Test
    public void testFqNestedClass() {
        assertThat(builder.appendFullyQualifiedClassName(Foo.class).build())
            .isEqualTo("org/jdbi/v3/core/locator/internal/TestClasspathBuilder$Foo");
    }

    public static class Foo {
        public void bar() {}
    }
}
