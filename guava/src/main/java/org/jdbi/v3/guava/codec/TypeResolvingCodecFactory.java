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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.reflect.TypeToken;
import com.google.common.reflect.TypeToken.TypeSet;
import com.google.errorprone.annotations.ThreadSafe;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.codec.CodecFactory;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Beta;

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
     */
    public static Builder builder() {
        return new Builder(TypeResolvingCodecFactory::new);
    }

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
