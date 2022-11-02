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
package org.jdbi.v3.core.codec;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestCodecFactory {

    private static final QualifiedType<Set<String>> SET_CODEC_TYPE = QualifiedType.of(new GenericType<Set<String>>() {});
    private static final QualifiedType<HashSet<String>> CONCRETE_TYPE_CODEC_TYPE = QualifiedType.of(new GenericType<HashSet<String>>() {});

    @Test
    public void testCollectionCodec() {
        CodecFactory factory = CodecFactory.forSingleCodec(SET_CODEC_TYPE, new GenericSetCodec());

        Optional<Function<Object, Argument>> result = factory.prepare(SET_CODEC_TYPE, new ConfigRegistry());
        assertThat(result).isPresent();

        // The TypeResolvingCodecFactory in jdbi3-guava returns true here.
        result = factory.prepare(CONCRETE_TYPE_CODEC_TYPE, new ConfigRegistry());
        assertThat(result).isNotPresent();
    }

    public static class GenericSetCodec implements Codec<Set<String>> {

        @Override
        public Function<Set<String>, Argument> getArgumentFunction() {
            return a -> (Argument) (position, statement, ctx) -> {};
        }
    }
}
