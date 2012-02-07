package org.skife.jdbi.v2;

import java.sql.ResultSet;

public class FoldController
{
    private boolean abort = false;
    private final ResultSet resultSet;

    public FoldController(ResultSet rs)
    {
        resultSet = rs;
    }

    public void abort() {
        this.abort  = true;
    }

    boolean isAborted()
    {
        return abort;
    }

    public ResultSet getResultSet()
    {
        return resultSet;
    }
}
