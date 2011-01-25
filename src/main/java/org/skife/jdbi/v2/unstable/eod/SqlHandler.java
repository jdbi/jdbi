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
        this.sql = method.getRawMember().getAnnotation(Sql.class).value();
    }

    public Object invoke(Handle h, Object[] args)
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
