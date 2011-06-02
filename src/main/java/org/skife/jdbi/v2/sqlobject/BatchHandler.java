package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.PreparedBatch;
import org.skife.jdbi.v2.PreparedBatchPart;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BatchHandler extends CustomizingStatementHandler
{
    private final String sql;

    public BatchHandler(Class sqlObjectType, ResolvedMethod method)
    {
        super(sqlObjectType, method);
        this.sql = SqlObject.getSql(method.getRawMember().getAnnotation(Batch.class), method.getRawMember());

    }

    public Object invoke(HandleDing h, Object target, Object[] args)
    {
        PreparedBatch batch = h.getHandle().prepareBatch(sql);

        List<Iterator> extras = new ArrayList<Iterator>();
        for (final Object arg : args) {
            if (arg instanceof Iterable) {
                extras.add(((Iterable) arg).iterator());
            }
            else if (arg instanceof Iterator) {
                extras.add((Iterator)arg);
            }
            else {
                extras.add(new Iterator()
                {
                    public boolean hasNext()
                    {
                        return true;
                    }

                    public Object next()
                    {
                        return arg;
                    }

                    public void remove()
                    {
                        // NOOP
                    }
                }
                );
            }
        }

        Object[] _args = null;
        while ((_args = next(extras)) != null) {
            PreparedBatchPart part = batch.add();
            applyBinders(part, _args);
            applyCustomizers(part, _args);
            try {
                applySqlStatementCustomizers(part, _args);
            }
            catch (SQLException e) {
                throw new UnableToCreateStatementException("exception raised in statement customizer application", e);
            }
        }


        return batch.execute();
    }

    private static Object[] next(List<Iterator> args)
    {
        List<Object> rs = new ArrayList<Object>();
        for (Iterator arg : args) {
            if (arg.hasNext()) {
                rs.add(arg.next());
            }
            else {
                return null;
            }
        }
        return rs.toArray();
    }
}
