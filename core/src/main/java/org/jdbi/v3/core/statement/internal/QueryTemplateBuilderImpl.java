package org.jdbi.v3.core.statement.internal;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.QueryTemplateBuilder;

public class QueryTemplateBuilderImpl implements QueryTemplateBuilder {

    private final ConfigRegistry config;
    final String sql;

    public QueryTemplateBuilderImpl(final Jdbi jdbi, final CharSequence sql) {
        this.sql = sql.toString();
        config = jdbi.getConfig().createCopy();
    }

    @Override
    public ConfigRegistry getConfig() {
        return config;
    }

    @Override
    public String getSql() {
        return sql;
    }
}
