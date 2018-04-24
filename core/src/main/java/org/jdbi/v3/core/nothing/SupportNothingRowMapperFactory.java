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
package org.jdbi.v3.core.nothing;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;

public class SupportNothingRowMapperFactory extends SupportNothingBase implements RowMapperFactory {
    public SupportNothingRowMapperFactory(Function<Class<?>, ? extends RuntimeException> provider) {
        super(provider);
    }

    @Override
    public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
        throw super.provider.apply(getClass());
    }
}
