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
package org.jdbi.guava.codec;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.reflect.TypeToken;
import com.google.common.reflect.TypeToken.TypeSet;
import com.google.errorprone.annotations.ThreadSafe;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jdbi.core.codec.Codec;
import org.jdbi.core.codec.CodecFactory;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.meta.Beta;

/**
 * An extended {@link CodecFactory} which can resolve Codecs for subtypes. This allows registering e.g. a codec for <code>Set&lt;String&gt;</code> and using any
 * subtype of {@link Set} which the same type parameters (e.g. a <code>HashSet&lt;String&lt;</code>) will find the right codec.
 * <p>
 * This is an experimental feature which relies on {@link TypeToken} from the Guava library.
 */
@Beta
@ThreadSafe
public class TypeResolvingCodecFactory extends CodecFactory {

    /**
     * Returns a builder for fluent API.
     * @return A {@link CodecFactory.Builder} instance.
     */
    @SuppressFBWarnings("HSM_HIDING_METHOD")
    public static Builder builder() {
        return new Builder(TypeResolvingCodecFactory::new);
    }

    /**
     * Creates a {@link CodecFactory} for a single type.
     * @param type The type for which the factory is created.
     * @param codec The {@link Codec} to use.
     * @return A new {@link CodecFactory} that will be used if the given type is requested.
     */
    @SuppressFBWarnings("HSM_HIDING_METHOD")
    public static CodecFactory forSingleCodec(QualifiedType<?> type, Codec<?> codec) {
        return new TypeResolvingCodecFactory(Collections.singletonMap(type, codec));
    }

    /**
     * Create a new TypeResolvingCodecFactory.
     */
    public TypeResolvingCodecFactory(final Map<QualifiedType<?>, Codec<?>> codecMap) {
        super(codecMap);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Codec<?> resolveType(QualifiedType<?> qualifiedType) {

        // memoize any successful lookup to avoid paying the cost for looking up
        // a mapping multiple times.
        return codecMap.computeIfAbsent(qualifiedType, q -> {
            Codec<?> codec = null;
            TypeSet candidates = TypeToken.of(q.getType()).getTypes();

            for (TypeToken candidate : (Set<TypeToken>) candidates) {
                QualifiedType<?> type = QualifiedType.of(candidate.getType());
                codec = codecMap.get(type);
                if (codec != null) {
                    break;
                }
            }
            return codec;
        });
    }
}
