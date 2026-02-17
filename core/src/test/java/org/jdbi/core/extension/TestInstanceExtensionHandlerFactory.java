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
package org.jdbi.core.extension;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestInstanceExtensionHandlerFactory {

    @Test
    public void testAcceptingClassMethods() throws Exception {

        Method abstractTestClassMethod = AbstractTestClass.class.getMethod("testMethod");
        Method concreteTestClassMethod = AbstractTestClass.class.getMethod("concreteMethod");

        // accept methods from an abstract class...
        assertThat(InstanceExtensionHandlerFactory.INSTANCE.accepts(AbstractTestClass.class, abstractTestClassMethod)).isTrue();
        assertThat(InstanceExtensionHandlerFactory.INSTANCE.accepts(AbstractTestClass.class, concreteTestClassMethod)).isTrue();
    }

    @Test
    public void testRejectObjectMethods() throws Exception {

        // do not accept methods declared by Object
        Method objectMethod = Object.class.getMethod("toString");
        assertThat(InstanceExtensionHandlerFactory.INSTANCE.accepts(AbstractTestClass.class, objectMethod)).isFalse();
    }

    @Test
    public void testAcceptingInterfaceMethods() throws Exception {

        Method testInterfaceMethod = TestInterface.class.getMethod("testMethod");
        Method defaultTestInterfaceMethod = TestInterface.class.getMethod("defaultMethod");

        // accept methods from the interface...
        assertThat(InstanceExtensionHandlerFactory.INSTANCE.accepts(TestInterface.class, testInterfaceMethod)).isTrue();
        // ... but not default methods
        assertThat(InstanceExtensionHandlerFactory.INSTANCE.accepts(TestInterface.class, defaultTestInterfaceMethod)).isFalse();
    }

    interface TestInterface {
        int testMethod();

        default int defaultMethod() {
            return 0;
        }
    }
    abstract static class AbstractTestClass {
        public abstract int testMethod();

        public int concreteMethod() {
            return 0;
        }
    }
}
