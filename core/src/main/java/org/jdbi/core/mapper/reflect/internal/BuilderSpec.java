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
package org.jdbi.core.mapper.reflect.internal;

import java.lang.reflect.Type;
import java.util.function.Supplier;

import org.jdbi.core.config.ConfigRegistry;

public class BuilderSpec<T, B> {
    Type type;
    ConfigRegistry config;
    Class<T> defn;
    Supplier<B> builder;

    BuilderSpec(Type type, ConfigRegistry config, Class<T> defn, Supplier<B> builder) {
        this.type = type;
        this.config = config;
        this.defn = defn;
        this.builder = builder;
    }
}
