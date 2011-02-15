package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class SqlReturningSingleObjectHandler implements Handler
{

    private final List<Binder> binders = new ArrayList<Binder>();

    private final ResultSetMapper mapper;
    private final String          sql;

    public SqlReturningSingleObjectHandler(ResolvedMethod method, Mapamajig mapamajig)
    {
        this.mapper = mapamajig.mapperFor(method, method.getReturnType());
        this.sql = method.getRawMember().getAnnotation(Sql.class).value();

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

    }

    public Object invoke(Handle h, Object[] args)
    {
        Query q = h.createQuery(sql);
        for (Binder binder : binders) {
            binder.bind(q, args);
        }
        return q.map(mapper).first();
    }
}
