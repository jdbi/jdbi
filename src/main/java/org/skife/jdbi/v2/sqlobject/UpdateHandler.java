package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Update;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

class UpdateHandler extends CustomizingStatementHandler
{
    final List<Bindifier> binders = new ArrayList<Bindifier>();
    final String sql;

    public UpdateHandler(ResolvedMethod method)
    {
        super(method);
        this.sql = method.getRawMember().getAnnotation(SqlUpdate.class).value();

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

    public Object invoke(HandleDing h, Object target, Object[] args)
    {
        Update q = h.getHandle().createStatement(sql);
        for (Bindifier binder : binders) {
            binder.bind(q, args);
        }
        applyCustomizers(q,args);
        return q.execute();
    }
}
