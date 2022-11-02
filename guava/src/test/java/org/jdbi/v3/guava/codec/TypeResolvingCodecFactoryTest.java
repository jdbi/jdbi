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
package org.jdbi.v3.guava.codec;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.codec.CodecFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TypeResolvingCodecFactoryTest {

    private static final QualifiedType<Set<String>> SET_CODEC_TYPE = QualifiedType.of(new GenericType<Set<String>>() {});
    private static final QualifiedType<HashSet<String>> CONCRETE_TYPE_CODEC_TYPE = QualifiedType.of(new GenericType<HashSet<String>>() {});

    private static final QualifiedType<XInterface<String>> XINTERFACE_CODEC_TYPE = QualifiedType.of(new GenericType<XInterface<String>>() {});

    @Test
    public void testCollectionCodec() {
        CodecFactory factory = TypeResolvingCodecFactory.forSingleCodec(SET_CODEC_TYPE, new GenericSetCodec());

        Optional<Function<Object, Argument>> result = factory.prepare(SET_CODEC_TYPE, new ConfigRegistry());
        assertThat(result).isPresent();

        result = factory.prepare(CONCRETE_TYPE_CODEC_TYPE, new ConfigRegistry());
        assertThat(result).isPresent();
    }

    @Test
    public void testInterfaceCodec() {
        CodecFactory factory = TypeResolvingCodecFactory.forSingleCodec(XINTERFACE_CODEC_TYPE, new StringXInterfaceCodec());

        QualifiedType<?> xClassType = QualifiedType.of(XClass.class);
        Optional<Function<Object, Argument>> result = factory.prepare(xClassType, new ConfigRegistry());
        assertThat(result).isPresent();

        QualifiedType<?> yClassType = QualifiedType.of(YClass.class);
        result = factory.prepare(yClassType, new ConfigRegistry());
        assertThat(result).isPresent();

        QualifiedType<?> zClassType = QualifiedType.of(ZClass.class);
        result = factory.prepare(zClassType, new ConfigRegistry());
        assertThat(result).isPresent();
    }

    @Test
    public void testNegativeStillWorks() {
        CodecFactory factory = TypeResolvingCodecFactory.forSingleCodec(SET_CODEC_TYPE, new GenericSetCodec());

        QualifiedType<Set<Integer>> integerSetType = QualifiedType.of(new GenericType<Set<Integer>>() {});
        Optional<Function<Object, Argument>> result = factory.prepare(integerSetType, new ConfigRegistry());
        assertThat(result).isNotPresent();

        QualifiedType<HashSet<Integer>> concreteIntegerType = QualifiedType.of(new GenericType<HashSet<Integer>>() {});
        result = factory.prepare(concreteIntegerType, new ConfigRegistry());
        assertThat(result).isNotPresent();

        QualifiedType<String> stringType = QualifiedType.of(String.class);
        result = factory.prepare(stringType, new ConfigRegistry());
        assertThat(result).isNotPresent();
    }

    public static class GenericSetCodec implements Codec<Set<String>> {

        @Override
        public Function<Set<String>, Argument> getArgumentFunction() {
            return a -> (Argument) (position, statement, ctx) -> {};
        }
    }

    public interface XInterface<X> {

        X getX();
    }

    public interface YInterface<Y> extends XInterface<String> {

        default String getX() {
            return null;
        }

        Y getY();
    }

    public interface ZInterface<Z, T> extends XInterface<T> {

        Z getZ();
    }

    public static class XClass implements XInterface<String> {

        public String getX() {
            return null;
        }
    }

    public static class YClass implements YInterface<Integer> {

        public String getX() {
            return null;
        }

        public Integer getY() {
            return null;
        }
    }

    public static class ZClass implements ZInterface<Boolean, String> {

        @Override
        public String getX() {
            return null;
        }

        @Override
        public Boolean getZ() {
            return null;
        }
    }

    public static class StringXInterfaceCodec implements Codec<XInterface<String>> {

        @Override
        public Function<XInterface<String>, Argument> getArgumentFunction() {
            return a -> (Argument) (position, statement, ctx) -> {};
        }
    }
}
