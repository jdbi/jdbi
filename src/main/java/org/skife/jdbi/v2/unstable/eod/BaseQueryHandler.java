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
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

abstract class BaseQueryHandler implements Handler
{
    private final List<Bindifier> binders = new ArrayList<Bindifier>();

    private final ResultSetMapper mapper;
    private final String          sql;
    private final ResolvedMethod method;

    public BaseQueryHandler(ResolvedMethod method, Mapamajig mapamajig)
    {
        this.method = method;
        this.mapper = mapamajig.mapperFor(method, mapTo());
        this.sql = method.getRawMember().getAnnotation(SqlQuery.class).value();

        Annotation[][] param_annotations = method.getRawMember().getParameterAnnotations();
        for (int param_idx = 0; param_idx < param_annotations.length; param_idx++) {
            Annotation[] annotations = param_annotations[param_idx];
            for (Annotation annotation : annotations) {
                if (Bind.class.isAssignableFrom(annotation.getClass())) {
                    Bind bind = (Bind) annotation;
                    try {
                        binders.add(new Bindifier(bind, param_idx, bind.binder().newInstance()));
                    }
                    catch (Exception e) {
                        throw new IllegalStateException("unable to instantiate specified binder", e);
                    }
                }
            }
        }
    }

    public Object invoke(Handle h, Object[] args)
    {
        Query q = h.createQuery(sql);
        for (Bindifier binder : binders) {
            binder.bind(q, args);
        }
        return resultType(q.map(mapper));

    }

    protected abstract Object resultType(org.skife.jdbi.v2.Query q);

    protected abstract ResolvedType mapTo();

    protected ResolvedMethod getMethod()
    {
        return method;
    }
}
