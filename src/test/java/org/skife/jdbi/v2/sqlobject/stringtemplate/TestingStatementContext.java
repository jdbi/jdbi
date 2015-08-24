/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2.sqlobject.stringtemplate;

import org.skife.jdbi.v2.Binding;
import org.skife.jdbi.v2.Cleanable;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

public class TestingStatementContext implements StatementContext
{
    private final Map<String, Object> attributes;

    public TestingStatementContext(final Map<String, Object> globalAttributes)
    {
        attributes = new HashMap<String, Object>(globalAttributes);
    }

    @Override
    public Object setAttribute(final String key, final Object value)
    {
        return attributes.put(key, value);
    }

    @Override
    public Object getAttribute(final String key)
    {
        return attributes.get(key);
    }

    @Override
    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    @Override
    public ResultSetMapper mapperFor(Class type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRawSql()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRewrittenSql()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLocatedSql()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public PreparedStatement getStatement()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection getConnection()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Binding getBinding()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getSqlObjectType()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Method getSqlObjectMethod()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReturningGeneratedKeys()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addCleanable(final Cleanable cleanable)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConcurrentUpdatable() {
        throw new UnsupportedOperationException();
    }

}
