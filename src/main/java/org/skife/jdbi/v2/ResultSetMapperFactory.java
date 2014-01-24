/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
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
package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.ResultSetMapper;

/**
 * Factory interface used to produce result set mappers.
 */
public interface ResultSetMapperFactory
{
    /**
     * Can this factory provide a result set mapper which maps to the desired type
     * @param type the target type to map to
     * @return true if it can, false if it cannot
     */
    boolean accepts(Class type, StatementContext ctx);

    /**
     * Supplies a result set mapper which will map result sets to type
     */
    ResultSetMapper mapperFor(Class type, StatementContext ctx);
}
