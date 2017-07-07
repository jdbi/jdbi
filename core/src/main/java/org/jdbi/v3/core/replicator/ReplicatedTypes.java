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
package org.jdbi.v3.core.replicator;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.RowMapper;

/**
 * Configuration object for the replicator.
 */
public class ReplicatedTypes implements JdbiConfig<ReplicatedTypes> {

    private final Map<Type, Optional<RowMapper<?>>> replicatedTypes = new HashMap<>();

    public ReplicatedTypes() { }
    public ReplicatedTypes(ReplicatedTypes other) {
        replicatedTypes.putAll(other.replicatedTypes);
    }

    @Override
    public ReplicatedTypes createCopy() {
        return new ReplicatedTypes(this);
    }

    Optional<RowMapper<?>> mapperFor(Type type) {
        return replicatedTypes.computeIfAbsent(type, this::constructMapper);
    }

    private Optional<RowMapper<?>> constructMapper(Type type) {
        if (GenericTypes.getErasedType(type).getAnnotation(Replicated.class) == null) {
            return Optional.empty();
        }
        return Optional.of(new ReplicatorMapper(type));
    }
}
