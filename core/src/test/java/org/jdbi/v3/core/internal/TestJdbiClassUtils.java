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
package org.jdbi.v3.core.internal;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.jdbi.v3.core.internal.JdbiClassUtils.MethodHandleHolder;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestJdbiClassUtils {

    @Test
    void testVarargs() {
        VarargsThing varargsThing = new VarargsThing();

        assertThat(varargsThing.varargs()).isInstanceOf(Object[].class).isEmpty();
        assertThat(varargsThing.varargs("x")).isInstanceOf(Object[].class).hasSize(1);
        assertThat(varargsThing.varargs("x", "y")).isInstanceOf(Object[].class).hasSize(2);
        assertThat(varargsThing.varargs((Object) null)).isInstanceOf(Object[].class).hasSize(1);
        assertThat(varargsThing.varargs((Object[]) null)).isNull();
    }

    @Test
    void testSafeVarargs() {
        VarargsThing varargsThing = new VarargsThing();

        assertThat(varargsThing.safeVarargs()).isInstanceOf(Object[].class).isEmpty();
        assertThat(varargsThing.safeVarargs("x")).isInstanceOf(Object[].class).hasSize(1);
        assertThat(varargsThing.safeVarargs("x", "y")).isInstanceOf(Object[].class).hasSize(2);
        assertThat(varargsThing.safeVarargs((Object) null)).isInstanceOf(Object[].class).hasSize(1);
        assertThat(varargsThing.safeVarargs((Object[]) null)).isInstanceOf(Object[].class).isEmpty();
    }

    @Test
    void testVarargsProxyMethod() throws Exception {

        VarargsThing varargsThing = new VarargsThing();

        Method varargsMethod = VarargsThing.class.getMethod("varargs", Object[].class);
        Method safeVarargsMethod = VarargsThing.class.getMethod("safeVarargs", Object[].class);

        VarargsInterface thing = (VarargsInterface) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[] {VarargsInterface.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("varargs")) {
                        return varargsMethod.invoke(varargsThing, args[0]);
                    } else if (method.getName().equals("safeVarargs")) {
                        return safeVarargsMethod.invoke(varargsThing, args[0]);
                    }
                    throw new IllegalStateException();
                });

        assertThat(thing.varargs()).isInstanceOf(Object[].class).isEmpty();
        assertThat(thing.varargs("x")).isInstanceOf(Object[].class).hasSize(1);
        assertThat(thing.varargs("x", "y")).isInstanceOf(Object[].class).hasSize(2);
        assertThat(thing.varargs((Object) null)).isInstanceOf(Object[].class).hasSize(1);
        assertThat(thing.varargs((Object[]) null)).isNull();

        assertThat(thing.safeVarargs()).isInstanceOf(Object[].class).isEmpty();
        assertThat(thing.safeVarargs("x")).isInstanceOf(Object[].class).hasSize(1);
        assertThat(thing.safeVarargs("x", "y")).isInstanceOf(Object[].class).hasSize(2);
        assertThat(thing.safeVarargs((Object) null)).isInstanceOf(Object[].class).hasSize(1);
        assertThat(thing.safeVarargs((Object[]) null)).isInstanceOf(Object[].class).isEmpty();
    }

    static class VarargsThing {

        VarargsThing() {}

        public Object[] varargs(Object... args) {
            return args;
        }

        public Object[] safeVarargs(Object... args) {
            return JdbiClassUtils.safeVarargs(args);
        }
    }

    interface VarargsInterface {

        Object[] varargs(Object... args);

        Object[] safeVarargs(Object... args);
    }

    @Test
    void testCreateCheckedInstance() {
        CCTestClass test1 = JdbiClassUtils.checkedCreateInstance(CCTestClass.class);
        assertThat(test1).isNotNull();
        assertThat(test1).extracting("strParam").isNull();
        assertThat(test1).extracting("intParam").isEqualTo(-1);

        CCTestClass test2 = JdbiClassUtils.checkedCreateInstance(CCTestClass.class, new Class[] {String.class}, "foo");
        assertThat(test2).isNotNull();
        assertThat(test2).extracting("strParam").isEqualTo("foo");
        assertThat(test2).extracting("intParam").isEqualTo(-1);

        CCTestClass test3 = JdbiClassUtils.checkedCreateInstance(CCTestClass.class, new Class[] {int.class, String.class}, 200, "foo");
        assertThat(test3).isNotNull();
        assertThat(test3).extracting("strParam").isEqualTo("foo");
        assertThat(test3).extracting("intParam").isEqualTo(200);
    }

    static class CCTestClass {

        private final String strParam;
        private final int intParam;

        public CCTestClass() {
            this.intParam = -1;
            this.strParam = null;
        }

        CCTestClass(String strParam) {
            this.strParam = strParam;
            this.intParam = -1;
        }

        CCTestClass(int intParam, String strParam) {
            this.strParam = strParam;
            this.intParam = intParam;
        }

        String strParam() {
            return strParam;
        }

        int intParam() {
            return intParam;
        }
    }

    @Test
    void testFindConstructor() {
        TestFCZero testFcZero = create(TestFCZero.class);
        assertThat(testFcZero).isNotNull();

        TestFCOne testFcOne = create(TestFCOne.class);
        assertThat(testFcOne).isNotNull();
        assertThat(testFcOne.str()).isEqualTo("one");

        TestFCTwo testFcTwo = create(TestFCTwo.class);
        assertThat(testFcTwo).isNotNull();
        assertThat(testFcTwo.str()).isEqualTo("one");
        assertThat(testFcTwo.i()).isEqualTo(2);

        TestFCThree testFcThree = create(TestFCThree.class);
        assertThat(testFcThree).isNotNull();
        assertThat(testFcThree.str()).isEqualTo("one");
        assertThat(testFcThree.i()).isEqualTo(2);
        assertThat(testFcThree.bool()).isEqualTo(true);
    }

    static final Class<?>[] CLASS_PARAMETERS = {String.class, int.class, boolean.class};

    @Test
    void testFailConstructor() {
        MethodHandleHolder<TestFCFail> failingHandle = JdbiClassUtils.findConstructor(TestFCFail.class, CLASS_PARAMETERS);
        assertThat(failingHandle).isNotNull();

        assertThatThrownBy(() -> failingHandle.invoke(handle -> handle.invokeExact("one", 2, true)))
                .isInstanceOf(NoSuchMethodException.class)
                .hasMessageContaining(format("No constructor for class '%s', loosely matching arguments %s", TestFCFail.class.getName(), Arrays.toString(CLASS_PARAMETERS)));
    }

    private static <T> T create(Class<T> clazz) {
        return JdbiClassUtils.findConstructorAndCreateInstance(clazz, CLASS_PARAMETERS,
                handle -> handle.invokeExact("one", 2, true));
    }

    static class TestFCZero {

        public TestFCZero() {}
    }

    static class TestFCOne {

        private final String str;

        public TestFCOne(String str) {
            this.str = str;
        }

        public String str() {
            return str;
        }
    }

    static class TestFCTwo {

        private final String str;
        private final int i;

        public TestFCTwo(String str, int i) {
            this.str = str;
            this.i = i;
        }

        public String str() {
            return str;
        }

        public int i() {
            return i;
        }
    }

    static class TestFCThree {

        private final String str;
        private final int i;
        private final boolean bool;

        public TestFCThree(String str, int i, boolean bool) {
            this.str = str;
            this.i = i;
            this.bool = bool;
        }

        public String str() {
            return str;
        }

        public int i() {
            return i;
        }

        public boolean bool() {
            return bool;
        }
    }
    static class TestFCFail {

        private final String str;
        private final int i;
        private final boolean bool;

        public TestFCFail(boolean bool, String str, int i) {
            this.bool = bool;
            this.str = str;
            this.i = i;
        }

        public String str() {
            return str;
        }

        public int i() {
            return i;
        }

        public boolean bool() {
            return bool;
        }
    }
}
