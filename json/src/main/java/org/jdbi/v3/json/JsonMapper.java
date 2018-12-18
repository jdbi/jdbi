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
package org.jdbi.v3.json;

import java.lang.reflect.Type;

import org.jdbi.v3.core.statement.StatementContext;

/**
 * Implement this interface and {@link JsonConfig#setJsonMapper(JsonMapper)} it
 * to be able to convert objects to/from JSON between your application and database.
 *
 * jdbi3-jackson2 and jdbi3-gson2 are readily available for this.
 */
public interface JsonMapper {
    String toJson(Type type, Object value, StatementContext ctx);
    Object fromJson(Type type, String json, StatementContext ctx);
}
