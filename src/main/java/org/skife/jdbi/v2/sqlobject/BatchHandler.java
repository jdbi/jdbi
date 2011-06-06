package org.skife.jdbi.v2.sqlobject;

import com.fasterxml.classmate.members.ResolvedMethod;
import com.sun.xml.internal.ws.handler.HandlerException;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.PreparedBatch;
import org.skife.jdbi.v2.PreparedBatchPart;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import sun.text.normalizer.IntTrie;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BatchHandler extends CustomizingStatementHandler
{
    private final String sql;
    private final boolean transactional;
    private final int batchChunkSize;

    public BatchHandler(Class sqlObjectType, ResolvedMethod method)
    {
        super(sqlObjectType, method);
        Method raw_method = method.getRawMember();
        SqlBatch anno = raw_method.getAnnotation(SqlBatch.class);
        this.sql = SqlObject.getSql(anno, raw_method);
        this.transactional = anno.transactional();
        if (method.getRawMember().isAnnotationPresent(BatchChunkSize.class)) {
            this.batchChunkSize = raw_method.getAnnotation(BatchChunkSize.class).value();
            if (this.batchChunkSize <= 0) {
                throw new IllegalArgumentException("Batch chunk size must be >= 0");
            }
        }
        else {
            // TODO check for batch chunk size on argument


            this.batchChunkSize = Integer.MAX_VALUE;
        }
    }

    public Object invoke(HandleDing h, Object target, Object[] args)
    {
        Handle handle = h.getHandle();

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

        int processed = 0;
        List<int[]> rs_parts = new ArrayList<int[]>();
        PreparedBatch batch = handle.prepareBatch(sql);
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

            if (++processed == batchChunkSize) {
                // execute this chunk
                processed = 0;
                rs_parts.add(executeBatch(handle, batch));
                batch = handle.prepareBatch(sql);
            }
        }

        //execute the rest
        rs_parts.add(executeBatch(handle, batch));

        // combine results
        int end_size = 0;
        for (int[] rs_part : rs_parts) {
            end_size += rs_part.length;
        }
        int[] rs = new int[end_size];
        int offset = 0;
        for (int[] rs_part : rs_parts) {
            System.arraycopy(rs_part, 0, rs, offset, rs_part.length);
            offset += rs_part.length;
        }

        return rs;
    }

    private int[] executeBatch(final Handle handle, final PreparedBatch batch)
    {
        if (transactional) {
            // it is safe to use same prepared batch as the inTransaction passes in the same
            // Handle instance.
            return handle.inTransaction(new TransactionCallback<int[]>() {
                public int[] inTransaction(Handle conn, TransactionStatus status) throws Exception
                {
                    return batch.execute();
                }
            });
        }
        else {
            return batch.execute();
        }
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
