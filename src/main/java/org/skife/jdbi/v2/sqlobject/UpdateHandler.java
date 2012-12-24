package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.members.ResolvedMethod;
import net.sf.cglib.proxy.MethodProxy;
import org.skife.jdbi.v2.ConcreteStatementContext;
import org.skife.jdbi.v2.GeneratedKeys;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Update;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

class UpdateHandler extends CustomizingStatementHandler
{
    private final String sql;
    private final Returner returner;

    public UpdateHandler(HandleDing handle, Class<?> sqlObjectType, ResolvedMethod method)
    {
        super(sqlObjectType, method);
        this.sql = SqlObject.getSql(method.getRawMember().getAnnotation(SqlUpdate.class), method.getRawMember());
        if (method.getRawMember().isAnnotationPresent(GetGeneratedKeys.class)) {

            final ResultReturnThing magic = ResultReturnThing.forType(method);
            final GetGeneratedKeys ggk = method.getRawMember().getAnnotation(GetGeneratedKeys.class);
            final ResultSetMapper mapper;
            try {
                mapper = ggk.value().newInstance();
            }
            catch (Exception e) {
                throw handle.getHandle().getExceptionPolicy().unableToCreateStatement("Unable to instantiate result set mapper for statement", e, null);
            }
            this.returner = new Returner()
            {
                public Object value(Update update, HandleDing baton)
                {
                    GeneratedKeys o = update.executeAndReturnGeneratedKeys(mapper);
                    return magic.result(o, baton);
                }
            };
        }
        else {
            this.returner = new Returner()
            {
                public Object value(Update update, HandleDing baton)
                {
                    return update.execute();
                }
            };
        }
    }

    public Object invoke(HandleDing h, Object target, Object[] args, MethodProxy mp)
    {
        Update q = h.getHandle().createStatement(sql);
        populateSqlObjectData((ConcreteStatementContext)q.getContext());
        applyCustomizers(q, args);
        applyBinders(q, args);
        return this.returner.value(q, h);
    }


    private interface Returner
    {
        Object value(Update update, HandleDing baton);
    }
}
