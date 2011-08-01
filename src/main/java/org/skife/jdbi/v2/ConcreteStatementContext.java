package org.skife.jdbi.v2;

import org.skife.jdbi.v2.StatementContext;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

public final class ConcreteStatementContext implements StatementContext
{
    private final Map<String, Object> attributes = new HashMap<String, Object>();
    private String            rawSql;
    private String            rewrittenSql;
    private String            locatedSql;
    private PreparedStatement statement;
    private Connection        connection;
    private Binding           binding;
    private Class<?>          sqlObjectType;
    private Method            sqlObjectMethod;
    private boolean           returningGeneratedKeys;

    ConcreteStatementContext(Map<String, Object> globalAttributes)
    {
        attributes.putAll(globalAttributes);
    }

    /**
     * Specify an attribute on the statement context
     *
     * @param key   name of the attribute
     * @param value value for the attribute
     *
     * @return previous value of this attribute
     */
    public Object setAttribute(String key, Object value)
    {
        return attributes.put(key, value);
    }

    /**
     * Obtain the value of an attribute
     *
     * @param key The name of the attribute
     *
     * @return the value of the attribute
     */
    public Object getAttribute(String key)
    {
        return this.attributes.get(key);
    }

    /**
     * Obtain all the attributes associated with this context as a map. Changes to the map
     * or to the attributes on the context will be reflected across both
     *
     * @return a map f attributes
     */
    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    void setRawSql(String rawSql)
    {
        this.rawSql = rawSql;
    }

    /**
     * Obtain the initial sql for the statement used to create the statement
     *
     * @return the initial sql
     */
    public String getRawSql()
    {
        return rawSql;
    }

    void setLocatedSql(String locatedSql)
    {
        this.locatedSql = locatedSql;
    }

    void setRewrittenSql(String rewrittenSql)
    {
        this.rewrittenSql = rewrittenSql;
    }

    /**
     * Obtain the located and rewritten sql
     * <p/>
     * Not available until until statement execution time
     *
     * @return the sql as it will be executed against the database
     */
    public String getRewrittenSql()
    {
        return rewrittenSql;
    }

    /**
     * Obtain the located sql
     * <p/>
     * Not available until until statement execution time
     *
     * @return the sql which will be passed to the statement rewriter
     */
    public String getLocatedSql()
    {
        return locatedSql;
    }

    void setStatement(PreparedStatement stmt)
    {
        statement = stmt;
    }

    /**
     * Obtain the actual prepared statement being used.
     * <p/>
     * Not available until execution time
     *
     * @return Obtain the actual prepared statement being used.
     */
    public PreparedStatement getStatement()
    {
        return statement;
    }

    void setConnection(Connection connection)
    {
        this.connection = connection;
    }

    /**
     * Obtain the JDBC connection being used for this statement
     *
     * @return the JDBC connection
     */
    public Connection getConnection()
    {
        return connection;
    }

    public void setBinding(Binding b)
    {
        this.binding = b;
    }

    public Binding getBinding()
    {
        return binding;
    }

    public final void setSqlObjectType(Class<?> sqlObjectType)
    {
        this.sqlObjectType = sqlObjectType;
    }

    public Class<?> getSqlObjectType()
    {
        return sqlObjectType;
    }

    public final void setSqlObjectMethod(Method sqlObjectMethod)
    {
        this.sqlObjectMethod = sqlObjectMethod;
    }

    public Method getSqlObjectMethod()
    {
        return sqlObjectMethod;
    }

    public void setReturningGeneratedKeys(boolean b)
    {
        this.returningGeneratedKeys = b;
    }

    public boolean isReturningGeneratedKeys()
    {
        return returningGeneratedKeys;
    }
}
