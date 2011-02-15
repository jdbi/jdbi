package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

abstract class BaseHandler implements Handler
{
    private final List<Bindifier> binders = new ArrayList<Bindifier>();

    private final ResultSetMapper mapper;
    private final String          sql;
    private final ResolvedMethod method;

    public BaseHandler(ResolvedMethod method, Mapamajig mapamajig)
    {
        this.method = method;
        this.mapper = mapamajig.mapperFor(method, mapTo());
        this.sql = method.getRawMember().getAnnotation(Sql.class).value();

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
        org.skife.jdbi.v2.Query q = h.createQuery(sql);
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
