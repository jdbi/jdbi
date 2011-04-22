package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.SQLStatement;

public interface SQLStatementCustomizer
{
    void apply(SQLStatement q);
}
