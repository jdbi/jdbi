package org.jdbi.v3.sqlobject.statement;

import java.util.function.Supplier;

import org.jdbi.v3.core.config.JdbiConfig;

public class SqlObjectStatementConfiguration implements JdbiConfig<SqlObjectStatementConfiguration>
{
    private Supplier<Object> returner;

    public SqlObjectStatementConfiguration() { }

    private SqlObjectStatementConfiguration(SqlObjectStatementConfiguration other)
    {
        this.returner = other.returner;
    }

    @Override
    public SqlObjectStatementConfiguration createCopy() {
        return new SqlObjectStatementConfiguration(this);
    }

    void setReturner(Supplier<Object> returner) {
        this.returner = returner;
    }

    Supplier<Object> getReturner() {
        return returner;
    }
}
