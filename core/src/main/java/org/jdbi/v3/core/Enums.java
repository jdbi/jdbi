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
import org.jdbi.v3.core.config.JdbiConfig;

/**
 * Configuration for behavior related to {@link Enum}s.
 */
public class Enums implements JdbiConfig<Enums> {
    private Class<? extends Annotation> defaultQualifier;

    public Enums() {
        defaultQualifier = EnumByName.class;
    }

    private Enums(Enums other) {
        defaultQualifier = other.defaultQualifier;
    }

    public Class<? extends Annotation> getDefaultQualifier() {
        return defaultQualifier;
    }

    public Enums defaultByName() {
        this.defaultQualifier = EnumByName.class;
        return this;
    }

    public Enums defaultByOrdinal() {
        this.defaultQualifier = EnumByOrdinal.class;
        return this;
    }

    @Override
    public Enums createCopy() {
        return new Enums(this);
    }
}
