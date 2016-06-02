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
package org.jdbi.v3;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.util.Types;
import org.junit.Test;

public class TypesTest {

    class Foo<T> {
    }

    @SuppressWarnings("rawtypes")
    Foo raw() {
        return null;
    }

    @Test
    public void getErasedTypeOfRaw() throws Exception {
        assertThat(Types.getErasedType(methodReturnType("raw")), equalTo(Foo.class));
    }

    @Test
    public void findGenericParameterOfRaw() throws Exception {
        assertThat(Types.findGenericParameter(methodReturnType("raw"), Foo.class), equalTo(empty()));
    }

    Foo<String> generic() {
        return null;
    }

    @Test
    public void getErasedTypeOfGeneric() throws Exception {
        assertThat(Types.getErasedType(methodReturnType("generic")), equalTo(Foo.class));
    }

    @Test
    public void findGenericParameterOfGeneric() throws Exception {
        assertThat(Types.findGenericParameter(methodReturnType("generic"), Foo.class),
                equalTo(Optional.of(String.class)));
    }

    Foo<Foo<String>> nestedGeneric() {
        return null;
    }

    @Test
    public void getErasedTypeOfNestedGeneric() throws Exception {
        assertThat(Types.getErasedType(methodReturnType("nestedGeneric")), equalTo(Foo.class));
    }

    @Test
    public void findGenericParameterOfNestedGeneric() throws Exception {
        assertThat(Types.findGenericParameter(methodReturnType("nestedGeneric"), Foo.class),
                equalTo(Optional.of(methodReturnType("generic"))));
    }

    class Bar<T> extends Foo<T> {
    }

    Bar<Integer> subTypeGeneric() {
        return null;
    }

    @Test
    public void findGenericParameterOfSuperClass() throws Exception {
        assertThat(Types.findGenericParameter(methodReturnType("subTypeGeneric"), Foo.class),
                equalTo(Optional.of(Integer.class)));
    }

    class Baz<T> extends Bar<T> {
    }

    Baz<String> descendentTypeGeneric() {
        return null;
    }

    @Test
    public void findGenericParameterOfAncestorClass() throws Exception {
        assertThat(Types.findGenericParameter(methodReturnType("descendentTypeGeneric"), Foo.class),
                equalTo(Optional.of(String.class)));
    }

    private Type methodReturnType(String methodName) throws NoSuchMethodException {
        return getClass().getDeclaredMethod(methodName).getGenericReturnType();
    }


    @Test
    public void resolveType() throws Exception {
        abstract class A<T> {
            abstract T a();
        }
        abstract class B extends A<String> {
        }

        assertThat(Types.resolveType(A.class.getDeclaredMethod("a").getGenericReturnType(), B.class),
                equalTo(String.class));
    }

    @Test
    public void resolveTypeUnrelatedContext() throws Exception {
        abstract class A1<T> {
            abstract T a();
        }
        abstract class A2<T> {
            abstract T a();
        }
        abstract class B extends A2<String> {
        }

        Type t = A1.class.getDeclaredMethod("a").getGenericReturnType();
        assertThat(Types.resolveType(t, B.class), equalTo(t));
    }
}
