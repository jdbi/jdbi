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
package org.jdbi.v3.spring;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.jdbi.v3.DBI;
import org.jdbi.v3.tweak.StatementLocator;
import org.springframework.beans.factory.FactoryBean;

import javax.annotation.PostConstruct;

/**
 * Utility class which constructs an {@link DBI} instance which can conveniently participate
 * in Spring's transaction management system.
 */
public class DBIFactoryBean implements FactoryBean<DBI>
{
    private DataSource dataSource;
    private StatementLocator statementLocator;
    private final Map<String, Object> globalDefines = new HashMap<>();

    public DBIFactoryBean() {
    }

    public DBIFactoryBean(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DBIFactoryBean(DataSource dataSource, StatementLocator statementLocator) {
        this.dataSource = dataSource;
        this.statementLocator = statementLocator;
    }

    /**
     * See {@link org.springframework.beans.factory.FactoryBean#getObject}
     */
    @Override
    public DBI getObject() throws Exception
    {
        final DBI dbi = DBI.create(new SpringDataSourceConnectionFactory(dataSource));
        if (statementLocator != null) {
            dbi.setStatementLocator(statementLocator);
        }

        for (Map.Entry<String, Object> entry : globalDefines.entrySet()) {
            dbi.define(entry.getKey(), entry.getValue());
        }

        return dbi;
    }

    /**
     * See {@link org.springframework.beans.factory.FactoryBean#getObjectType}
     */
    @Override
    public Class<?> getObjectType()
    {
        return DBI.class;
    }

    /**
     * See {@link org.springframework.beans.factory.FactoryBean#isSingleton}
     *
     * @return false
     */
    @Override
    public boolean isSingleton()
    {
        return true;
    }

    /**
     * The datasource, which should be managed by spring's transaction system, from which
     * the {@link DBI} will obtain connections
     */
    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public void setStatementLocator(StatementLocator statementLocator)
    {
        this.statementLocator = statementLocator;
    }

    public void setGlobalDefines(Map<String, Object> defines) {
        globalDefines.putAll(defines);
    }

    /**
     * Verifies that a dataSource has been set
     */
    @PostConstruct
    private void afterPropertiesSet()
    {
        if (dataSource == null) {
            throw new IllegalStateException("'dataSource' property must be set");
        }
    }
}
