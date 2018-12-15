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
package org.jdbi.v3.jackson2;

import java.io.IOException;
import java.lang.reflect.Type;

import org.jdbi.v3.core.result.UnableToProduceResultException;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.json.internal.JsonMapperImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class JacksonJsonImpl implements JsonMapperImpl {
    @Override
    public String toJson(Type type, Object value, StatementContext ctx) {
        try {
            return getMapper(ctx).writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new UnableToCreateStatementException(e, ctx);
        }
    }

    @Override
    public Object fromJson(Type type, String json, StatementContext ctx) {
        try {
            return getMapper(ctx).readValue(json, new TypeReference<Object>() {
                @Override
                public Type getType() {
                    return type;
                }
            });
        } catch (IOException e) {
            throw new UnableToProduceResultException(e, ctx);
        }
    }

    private ObjectMapper getMapper(StatementContext ctx) {
        return ctx.getConfig(Jackson2Config.class).getMapper();
    }
}
