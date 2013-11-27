/*
 * Copyright (C) 2004 - 2013 Brian McCallister
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
package org.skife.jdbi.v3.sqlobject;

import org.skife.jdbi.v3.PrimitivesMapperFactory;
import org.skife.jdbi.v3.StatementContext;
import org.skife.jdbi.v3.tweak.ResultSetMapper;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;

class FigureItOutResultSetMapper implements ResultSetMapper<Object>
{
    private static final PrimitivesMapperFactory factory = new PrimitivesMapperFactory();

    public Object map(int index, ResultSet r, StatementContext ctx) throws SQLException
    {
        Method m = ctx.getSqlObjectMethod();
        m.getAnnotation(GetGeneratedKeys.class);
        Class<?> rt = m.getReturnType();
        ResultSetMapper f = factory.mapperFor(rt, ctx);
        return f.map(index, r, ctx);
    }
}
