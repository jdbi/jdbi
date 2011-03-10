package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Update;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

class UpdateHandler extends CustomizingStatementHandler
{
    final String sql;

    public UpdateHandler(ResolvedMethod method)
    {
        super(method);
        this.sql = method.getRawMember().getAnnotation(SqlUpdate.class).value();
    }

    public Object invoke(HandleDing h, Object target, Object[] args)
    {
        Update q = h.getHandle().createStatement(sql);
        applyBinders(q, args);
        applyCustomizers(q,args);
        return q.execute();
    }
}
