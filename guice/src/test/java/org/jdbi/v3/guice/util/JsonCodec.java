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
package org.jdbi.v3.guice.util;

import java.io.IOException;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableMap;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;

@Singleton
public class JsonCodec implements Codec<ImmutableMap<String, Object>> {

    public static final QualifiedType<ImmutableMap<String, Object>> TYPE = QualifiedType.of(new GenericType<ImmutableMap<String, Object>>() {});

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());

    @Inject
    public JsonCodec() {}

    @Override
    public ColumnMapper<ImmutableMap<String, Object>> getColumnMapper() {
        return (rs, c, ctx) -> {
            try {
                ImmutableMap<String, Object> result = mapper.readValue(rs.getString(c), new TypeReference<ImmutableMap<String, Object>>() {});
                return result;
            } catch (IOException e) {
                throw Sneaky.throwAnyway(e);
            }
        };
    }

    @Override
    public Function<ImmutableMap<String, Object>, Argument> getArgumentFunction() {
        return attributes -> (Argument) (position, statement, ctx) -> {
            try {
                statement.setString(position, mapper.writeValueAsString(attributes));
            } catch (IOException e) {
                throw Sneaky.throwAnyway(e);
            }
        };
    }
}
