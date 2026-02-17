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
package org.jdbi.sqlobject.config.internal;

import java.lang.annotation.Annotation;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.SimpleExtensionConfigurer;
import org.jdbi.core.mapper.JoinRowMapper;
import org.jdbi.core.mapper.RowMappers;
import org.jdbi.sqlobject.config.RegisterJoinRowMapper;

public class RegisterJoinRowMapperImpl extends SimpleExtensionConfigurer {

    private final JoinRowMapper mapper;

    public RegisterJoinRowMapperImpl(Annotation annotation) {
        RegisterJoinRowMapper registerJoinRowMapper = (RegisterJoinRowMapper) annotation;
        this.mapper = JoinRowMapper.forTypes(registerJoinRowMapper.value());
    }

    @Override
    public void configure(ConfigRegistry config, Annotation annotation, Class<?> sqlObjectType) {
        config.get(RowMappers.class).register(mapper);
    }
}
