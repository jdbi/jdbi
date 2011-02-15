package org.skife.jdbi.v2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.skife.jdbi.v2.exceptions.ResultSetException;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class GeneratedKeys<Type>
{
    private final ResultSetMapper<Type> mapper;
    private final SQLStatement<?> jdbiStatement;
    private final Statement stmt;
    private final ResultSet results;
    private final StatementContext context;

    /**
     * Creates a new wrapper object for generated keys as returned by the {@link Statement#getGeneratedKeys()}
     * method for update and insert statement for drivers that support this function.
     *
     * @param mapper Maps the generated keys result set to an object
     * @param jdbiStatement The original jDBI statement
     * @param stmt The corresponding sql statement
     * @param context The statement context
     */
    public GeneratedKeys(ResultSetMapper<Type> mapper,
                         SQLStatement<?> jdbiStatement,
                         Statement stmt,
                         StatementContext context) throws SQLException
    {
        this.mapper = mapper;
        this.jdbiStatement = jdbiStatement;
        this.stmt = stmt;
        this.results = stmt.getGeneratedKeys();
        this.context = context;
    }

    /**
     * Returns the first generated key.
     *
     * @return The key or null if no keys were returned
     */
    public Type first() {
        try {
            if ((results != null) && results.next()) {
                return mapper.map(0, results, context);
            }
            else {
                // no result matches
                return null;
            }
        }
        catch (SQLException e) {
            throw new ResultSetException("Exception thrown while attempting to traverse the result set", e, context);
        }
        finally {
            QueryPostMungeCleanup.CLOSE_RESOURCES_QUIETLY.cleanup(jdbiStatement, null, results);
        }
    }

    /**
     * Returns a list of all generated keys.
     *
     * @return The list of keys or an empty list if no keys were returned
     */
    public List<Type> list() {
        try {
            List<Type> resultList = new ArrayList<Type>();

            if ((results != null) && !results.isClosed()) {
                int index = 0;
                while (results.next()) {
                    resultList.add(mapper.map(index++, results, context));
                }
            }
            return resultList;
        }
        catch (SQLException e) {
            throw new ResultSetException("Exception thrown while attempting to traverse the result set", e, context);
        }
        finally {
            QueryPostMungeCleanup.CLOSE_RESOURCES_QUIETLY.cleanup(jdbiStatement, null, results);
        }
    }

    /**
     * Returns a iterator over all generated keys.
     *
     * @return The key iterator
     */
    public ResultIterator<Type> iterator() {
        try {
            return new ResultSetResultIterator<Type>(mapper, jdbiStatement, stmt, context);
        }
        catch (SQLException e) {
            throw new ResultSetException("Exception thrown while attempting to traverse the result set", e, context);
        }
    }

    /**
     * Used to execute the query and traverse the generated keys with a accumulator.
     * <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Folding</a> over the
     * keys involves invoking a callback for each row, passing into the callback the return value
     * from the previous function invocation.
     *
     * @param accumulator The initial accumulator value
     * @param folder      Defines the function which will fold over the result set.
     *
     * @return The return value from the last invocation of {@link Folder#fold(Object, java.sql.ResultSet)}
     *
     * @see org.skife.jdbi.v2.Folder
     */
    public <AccumulatorType> AccumulatorType fold(AccumulatorType accumulator, final Folder2<AccumulatorType> folder)
    {
        try {
            AccumulatorType value = accumulator;

            if ((results != null) && !results.isClosed()) {
                while (results.next()) {
                    value = folder.fold(value, results, context);
                }
            }
            return value;
        }
        catch (SQLException e) {
            throw new ResultSetException("Exception thrown while attempting to traverse the result set", e, context);
        }
        finally {
            QueryPostMungeCleanup.CLOSE_RESOURCES_QUIETLY.cleanup(jdbiStatement, null, results);
        }
    }
}
