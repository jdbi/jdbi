package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

class SqlReturningQueryHandler implements Handler
{
    private final String sql;
    private final List<Binder> binders = new ArrayList<Binder>();
    private ResultSetMapper mapper;

    public SqlReturningQueryHandler(ResolvedMethod method, Mapamajig mappers)
    {
        sql = method.getRawMember().getAnnotation(Sql.class).value();

        Annotation[][] param_annotations = method.getRawMember().getParameterAnnotations();
        for (int param_idx = 0; param_idx < param_annotations.length; param_idx++) {
            Annotation[] annotations = param_annotations[param_idx];
            for (Annotation annotation : annotations) {
                if (Bind.class.isAssignableFrom(annotation.getClass())) {
                    Bind bind = (Bind) annotation;
                    binders.add(new Binder(bind, param_idx));
                }
            }
        }


        ResolvedType query_type = method.getReturnType();
        List<ResolvedType> query_return_types = query_type.typeParametersFor(Query.class);
        ResolvedType returnType = query_return_types.get(0);

        mapper = mappers.mapperFor(method, returnType);

    }

    public Object invoke(Handle h, Object[] args)
    {
        Query q = h.createQuery(sql);

        for (Binder binder : binders) {
            binder.bind(q, args);
        }

        return q.map(mapper);
    }
}
