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

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        assertThat(builder.appendVerbatim(" abc // / xyz ").build())
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
    public void testUnixPath() {
        Path path = Paths.get("/home/johndoe/docs");
        assertThat(builder.appendRelativePath(path).build())
            .isEqualTo("home/johndoe/docs");
    }

    @Test
    // cross-platform behavior, especially of absolute instead of relative paths, is unpredictable
    // but since you don't tend to run into windows paths on a unix system, it shouldn't matter
    public void testAbsoluteWindowsPath() {
        Path path = Paths.get("C:\\Program Files\\My Program");
        assertThat(builder.appendRelativePath(path).build())
            // windows vs linux interpretation
            .isIn("Program Files/My Program", "C:\\Program Files\\My Program");
    }

    @Test
    // cross-platform behavior, especially of absolute instead of relative paths, is unpredictable
    // but since you don't tend to run into windows paths on a unix system, it shouldn't matter
    public void testRelativeWindowsPath() {
        Path path = Paths.get("Program Files\\My Program");
        assertThat(builder.appendRelativePath(path).build())
            // windows vs linux interpretation
            .isIn("Program Files/My Program", "Program Files\\My Program");
    }

    @Test
    public void testDomain() {
        assertThat(builder.appendHostname(URI.create("https://john:doe@google.com:8080/foo")).build())
            .isEqualTo("com/google");
    }

    @Test
    public void testPackage() {
        assertThat(builder.appendPackage(String.class.getPackage()).build())
            .isEqualTo("java/lang");
    }

    @Test
    public void testSimpleClassWithExtension() {
        assertThat(builder.appendSimpleClassName(String.class).setExtension("java").build())
            .isEqualTo("String.java");
    }

    @Test
    public void testMethod() throws NoSuchMethodException {
        Method method = String.class.getMethod("join", CharSequence.class, CharSequence[].class);

        assertThat(builder.appendVerbatim("luke").appendMethodName(method).appendDotPath("the.dark.side").build())
            .isEqualTo("luke/join/the/dark/side");
    }

    @Test
    public void testSimpleClassAndMethod() throws NoSuchMethodException {
        Method method = String.class.getMethod("join", CharSequence.class, CharSequence[].class);

        assertThat(builder.appendSimpleClassAndMethodName(method).build())
            .isEqualTo("String/join");
    }

    @Test
    public void testFqClassAndMethod() throws NoSuchMethodException {
        Method method = String.class.getMethod("join", CharSequence.class, CharSequence[].class);

        assertThat(builder.appendFullyQualifiedMethodName(method).build())
            .isEqualTo("java/lang/String/join");
    }

    @Test
    public void testFqClass() {
        assertThat(builder.appendFullyQualifiedClassName(String.class).build())
            .isEqualTo("java/lang/String");
    }
}
