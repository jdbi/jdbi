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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
