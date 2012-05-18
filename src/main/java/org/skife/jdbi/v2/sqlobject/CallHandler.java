package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.members.ResolvedMethod;
import net.sf.cglib.proxy.MethodProxy;
import org.skife.jdbi.v2.Call;
import org.skife.jdbi.v2.ConcreteStatementContext;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.OutParameters;

class CallHandler extends CustomizingStatementHandler
{
    private final String sql;
    private final boolean returnOutParams;

    CallHandler(Class<?> sqlObjectType, ResolvedMethod method)
    {
        super(sqlObjectType, method);

        if (null != method.getReturnType() ) {
            if (method.getReturnType().isInstanceOf(OutParameters.class)){
                returnOutParams = true;
            }
            else {
                throw new IllegalArgumentException("@SqlCall methods may only return null or OutParameters at present");
            }
        }
        else {
            returnOutParams = false;
        }

        this.sql = SqlObject.getSql(method.getRawMember().getAnnotation(SqlCall.class), method.getRawMember());
    }

    @Override
    public Object invoke(HandleDing ding, Object target, Object[] args, MethodProxy mp)
    {
        Handle h = ding.getHandle();
        Call call = h.createCall(sql);
        populateSqlObjectData((ConcreteStatementContext)call.getContext());
        applyCustomizers(call, args);
        applyBinders(call, args);

        OutParameters ou = call.invoke();

        if (returnOutParams) {
            return ou;
        }
        else {
            return null;
        }
    }
}
