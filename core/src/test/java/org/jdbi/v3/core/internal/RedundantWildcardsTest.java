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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.generic.GenericType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RedundantWildcardsTest {

    enum Color { RED }

    enum Operation {
        PLUS {
            @Override
            int apply(int a, int b) {
                return a + b;
            }
        };

        abstract int apply(int a, int b);
    }

    record Point(int x) {}

    @SuppressWarnings("unused")
    static final class Holder<T> {}

    @SuppressWarnings("unused")
    static class Outer<T> {
        final class Inner {}

        class Generic<U> {}
    }

    @Test
    public void testFinalClassWildcard() {
        assertEquivalent(new GenericType<List<? extends String>>() {}, new GenericType<List<String>>() {});
    }

    @Test
    public void testFinalEnumWildcard() {
        assertEquivalent(new GenericType<List<? extends Color>>() {}, new GenericType<List<Color>>() {});
    }

    @Test
    public void testRecordWildcard() {
        assertEquivalent(new GenericType<List<? extends Point>>() {}, new GenericType<List<Point>>() {});
    }

    @Test
    public void testNestedWildcard() {
        assertEquivalent(new GenericType<Map<String, List<? extends String>>>() {}, new GenericType<Map<String, List<String>>>() {});
    }

    @Test
    public void testFinalParameterizedBound() {
        assertEquivalent(new GenericType<List<? extends Optional<? extends String>>>() {}, new GenericType<List<Optional<String>>>() {});
    }

    @Test
    public void testNestedRawType() {
        assertEquivalent(new GenericType<Map.Entry<String, ? extends String>>() {}, new GenericType<Map.Entry<String, String>>() {});
    }

    @Test
    public void testKeptWildcardBound() {
        assertEquivalent(new GenericType<List<? extends List<? extends String>>>() {}, new GenericType<List<? extends List<String>>>() {});
        assertEquivalent(
            new GenericType<Map<String, ? extends List<? extends String>>>() {},
            new GenericType<Map<String, ? extends List<String>>>() {});
    }

    @Test
    public void testLowerBound() {
        assertEquivalent(new GenericType<List<? super List<? extends String>>>() {}, new GenericType<List<? super List<String>>>() {});
        assertNotEquivalent(new GenericType<List<? super String>>() {}, new GenericType<List<String>>() {});
    }

    @Test
    public void testFinalArrayWildcard() {
        assertEquivalent(new GenericType<List<? extends String[]>>() {}, new GenericType<List<String[]>>() {});
        assertEquivalent(new GenericType<List<? extends int[][]>>() {}, new GenericType<List<int[][]>>() {});
    }

    @Test
    public void testGenericArrayComponent() {
        assertEquivalent(new GenericType<List<? extends String>[]>() {}, new GenericType<List<String>[]>() {});
        Type wildcard = returnType("wildcardOptionalArray");
        Type exact = returnType("optionalArray");
        assertThat(RedundantWildcards.equivalent(wildcard, exact)).isTrue();
        assertThat(RedundantWildcards.equivalent(exact, wildcard)).isTrue();
    }

    @SuppressWarnings("unused")
    private static List<? extends Optional<String>[]> wildcardOptionalArray() {
        return List.of();
    }

    @SuppressWarnings("unused")
    private static List<Optional<String>[]> optionalArray() {
        return List.of();
    }

    private static Type returnType(String method) {
        try {
            return RedundantWildcardsTest.class.getDeclaredMethod(method).getGenericReturnType();
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testOwnerType() {
        assertEquivalent(new GenericType<Outer<? extends String>.Generic<Integer>>() {}, new GenericType<Outer<String>.Generic<Integer>>() {});
    }

    @Test
    public void testFinalBoundWithWildcardArgument() {
        assertNotEquivalent(
            new GenericType<List<? extends Optional<? extends CharSequence>>>() {},
            new GenericType<List<Optional<? extends CharSequence>>>() {});
    }

    @Test
    public void testFinalBoundWithWildcardOwnerArgument() {
        assertNotEquivalent(
            new GenericType<List<? extends Outer<? extends CharSequence>.Inner>>() {},
            new GenericType<List<Outer<? extends CharSequence>.Inner>>() {});
        assertEquivalent(
            new GenericType<List<? extends Outer<? extends String>.Inner>>() {},
            new GenericType<List<Outer<String>.Inner>>() {});
    }

    @Test
    public void testFinalBoundWithWildcardArgumentOfOwnType() {
        assertNotEquivalent(
            new GenericType<List<? extends Holder<? extends CharSequence>>>() {},
            new GenericType<List<Holder<? extends CharSequence>>>() {});
    }

    @Test
    public void testEnumWithConstantBodies() {
        assertNotEquivalent(new GenericType<List<? extends Operation>>() {}, new GenericType<List<Operation>>() {});
    }

    @Test
    public void testInterfaceWildcard() {
        assertNotEquivalent(new GenericType<List<? extends CharSequence>>() {}, new GenericType<List<CharSequence>>() {});
    }

    @Test
    public void testAbstractClassWildcard() {
        assertNotEquivalent(new GenericType<List<? extends Number>>() {}, new GenericType<List<Number>>() {});
    }

    @Test
    public void testOpenArrayWildcard() {
        assertNotEquivalent(new GenericType<List<? extends Object[]>>() {}, new GenericType<List<Object[]>>() {});
        assertNotEquivalent(new GenericType<List<? extends CharSequence[]>>() {}, new GenericType<List<CharSequence[]>>() {});
    }

    @Test
    public void testUnboundedWildcard() {
        assertNotEquivalent(new GenericType<List<?>>() {}, new GenericType<List<Object>>() {});
    }

    @Test
    public void testDifferentTypes() {
        assertNotEquivalent(new GenericType<List<String>>() {}, new GenericType<List<Integer>>() {});
        assertNotEquivalent(new GenericType<List<? extends String>>() {}, new GenericType<Optional<String>>() {});
        assertNotEquivalent(new GenericType<Map<String, String>>() {}, new GenericType<Map<String, ? extends Integer>>() {});
    }

    @Test
    public void testTypeVariable() {
        Type typeVariable = List.class.getTypeParameters()[0];

        assertThat(RedundantWildcards.equivalent(typeVariable, typeVariable)).isTrue();
        assertThat(RedundantWildcards.equivalent(typeVariable, Object.class)).isFalse();
    }

    @Test
    public void testMatcher() {
        assertThat(RedundantWildcards.matcher(new GenericType<List<? extends String>>() {}.getType()))
            .accepts(new GenericType<List<String>>() {}.getType(), new GenericType<List<? extends String>>() {}.getType())
            .rejects(new GenericType<List<CharSequence>>() {}.getType(), List.class);
        Type wildcardString = ((ParameterizedType) new GenericType<List<? extends String>>() {}.getType()).getActualTypeArguments()[0];
        Type wildcardCharSequence = ((ParameterizedType) new GenericType<List<? extends CharSequence>>() {}.getType()).getActualTypeArguments()[0];
        assertThat(RedundantWildcards.matcher(String.class))
            .accepts(String.class, wildcardString)
            .rejects(CharSequence.class, new GenericType<List<String>>() {}.getType());
        assertThat(RedundantWildcards.matcher(CharSequence.class)).rejects(wildcardCharSequence);
    }

    private static void assertEquivalent(GenericType<?> a, GenericType<?> b) {
        assertThat(RedundantWildcards.equivalent(a.getType(), b.getType())).isTrue();
        assertThat(RedundantWildcards.equivalent(b.getType(), a.getType())).isTrue();
    }

    private static void assertNotEquivalent(GenericType<?> a, GenericType<?> b) {
        assertThat(RedundantWildcards.equivalent(a.getType(), b.getType())).isFalse();
        assertThat(RedundantWildcards.equivalent(b.getType(), a.getType())).isFalse();
    }
}
