/*
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
package org.jdbi.v3;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.tweak.ResultColumnMapper;

public class TestingStatementContext implements StatementContext
{
    private final Map<String, Object> attributes;
    private final MappingRegistry registry = new MappingRegistry();

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
    public <T> ResultColumnMapper<? extends T> columnMapperFor(Class<T> type) {
        return registry.columnMapperFor(type, this);
    }

    @Override
    public SqlName getSqlName()
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
    public String[] getGeneratedKeysColumnNames() {
        return new String[0];
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

    public void registerColumnMapper(ResultColumnMapper<?> m) {
        registry.addColumnMapper(m);
    }

}
