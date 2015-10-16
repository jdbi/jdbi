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
package org.jdbi.v3;

import org.jdbi.v3.tweak.ResultColumnMapper;

/**
 * Factory interface used to produce column mappers.
 */
public interface ResultColumnMapperFactory
{
    /**
     * Can this factory provide a column mapper which maps to the desired type
     * @param type the target type to map to
     * @return true if it can, false if it cannot
     */
    boolean accepts(Class type, StatementContext ctx);

    /**
     * Supplies a column mapper which will map result set columns to type
     */
    ResultColumnMapper columnMapperFor(Class type, StatementContext ctx);
}
