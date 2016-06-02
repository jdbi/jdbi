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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.jdbi.v3.Jdbi;
import org.jdbi.v3.spi.JdbiPlugin;
import org.jdbi.v3.statement.StatementLocator;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * Utility class which constructs an {@link Jdbi} instance which can conveniently participate
 * in Spring's transaction management system.
 */
public class DBIFactoryBean implements FactoryBean<Jdbi>
{
    private DataSource dataSource;
    private StatementLocator statementLocator;
    private final Map<String, Object> globalDefines = new HashMap<>();

    private Collection<JdbiPlugin> plugins = Collections.emptyList();

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
    public Jdbi getObject() throws Exception
    {
        final Jdbi dbi = Jdbi.create(() -> DataSourceUtils.getConnection(dataSource));

        plugins.forEach(dbi::installPlugin);

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
        return Jdbi.class;
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
     * the {@link Jdbi} will obtain connections
     *
     * @param dataSource the data source.
     */
    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public void setStatementLocator(StatementLocator statementLocator)
    {
        this.statementLocator = statementLocator;
    }

    @Autowired(required=false)
    public DBIFactoryBean setPlugins(Collection<JdbiPlugin> plugins)
    {
        this.plugins = plugins;
        return this;
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
