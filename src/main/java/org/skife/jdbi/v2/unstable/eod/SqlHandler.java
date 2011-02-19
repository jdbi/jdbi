/*
 * Copyright 2004 - 2011 Brian McCallister
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

package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Handle;

import java.lang.reflect.Method;
import java.util.List;

class SqlHandler implements Handler
{
    private final ResolvedMethod method;
    private final String sql;

    public SqlHandler(ResolvedMethod method)
    {
        this.method = method;
        this.sql = method.getRawMember().getAnnotation(SqlQuery.class).value();
    }

    public Object invoke(Handle h, Object target, Object[] args)
    {

        Method m = method.getRawMember();
        ResolvedType rt = method.getType();
        if (List.class.equals(m.getReturnType())) {
            final List<ResolvedType> ptypes = rt.typeParametersFor(List.class);
            if (ptypes.size() != 1) {
                throw new UnsupportedOperationException("Not Yet Implemented!");
            }
            ResolvedType elem_type = ptypes.get(0);
            if (elem_type.isInstanceOf(String.class)) {
                // List<String> so first elem, as string

            }

        }

        return new Object();
    }
}
