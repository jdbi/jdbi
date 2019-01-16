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
package org.jdbi.v3.core;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.jdbi.v3.core.config.JdbiConfig;

/**
 * Configuration for behavior related to {@link Enum}s.
 */
public class EnumConfig implements JdbiConfig<EnumConfig> {
    private static final Set<Class<? extends Annotation>> ALLOWED_HANDLINGS = new HashSet<>(Arrays.asList(EnumByName.class, EnumByOrdinal.class));
    private Class<? extends Annotation> handling;

    public EnumConfig() {
        handling = EnumByName.class;
    }

    private EnumConfig(EnumConfig other) {
        handling = other.handling;
    }

    /**
     * Applies to both binding and mapping.
     *
     * @return true if enums are handled by name, false if enums are handled by ordinal
     */
    @Deprecated
    public boolean isEnumHandledByName() {
        return handling == EnumByName.class;
    }

    /**
     * Applies to both binding and mapping.
     *
     * @param byName true if enums should be handled by name, false if enums should be handled by ordinal
     */
    @Deprecated
    public void setEnumHandledByName(boolean byName) {
        handling = byName ? EnumByName.class : EnumByOrdinal.class;
    }

    public Class<? extends Annotation> getEnumHandling() {
        return handling;
    }

    public EnumConfig setEnumHandling(Class<? extends Annotation> handling) {
        if (!ALLOWED_HANDLINGS.contains(handling)) {
            throw new IllegalArgumentException(handling + " is not an accepted enum handling annotation");
        }
        this.handling = handling;
        return this;
    }

    @Override
    public EnumConfig createCopy() {
        return new EnumConfig(this);
    }
}
