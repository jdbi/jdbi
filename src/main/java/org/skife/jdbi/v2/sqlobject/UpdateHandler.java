package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.Update;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;

import java.sql.SQLException;

class UpdateHandler extends CustomizingStatementHandler
{
    final String sql;

    public UpdateHandler(Class sqlObjectType, ResolvedMethod method)
    {
        super(sqlObjectType, method);
        this.sql = SqlObject.getSql(method.getRawMember().getAnnotation(SqlUpdate.class), method.getRawMember());
    }

    public Object invoke(HandleDing h, Object target, Object[] args)
    {
        Update q = h.getHandle().createStatement(sql);
        applyBinders(q, args);
        applyCustomizers(q, args);
        applySqlStatementCustomizers(q, args);

        return q.execute();
    }
}
