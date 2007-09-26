/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2.spring;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.util.Map;
import java.util.HashMap;

/**
 * Utility class which constructs an IDBI instance which can conveniently participate
 * in Spring's transaction management system.
 */
public class DBIFactoryBean implements FactoryBean, InitializingBean
{
    private DataSource dataSource;
    private StatementLocator statementLocator;
    private Map<String, Object> globalDefines = new HashMap<String, Object>();

    /**
     * See org.springframework.beans.factory.FactoryBean#getObject
     */
    public Object getObject() throws Exception
    {
        final DBI dbi = new DBI(new SpringDataSourceConnectionFactory(dataSource));
        if (statementLocator != null) {
            dbi.setStatementLocator(statementLocator);
        }

        for (Map.Entry<String, Object> entry : globalDefines.entrySet()) {
            dbi.define(entry.getKey(), entry.getValue());
        }

        return dbi;
    }

    /**
     * See org.springframework.beans.factory.FactoryBean#getObjectType
     */
    public Class getObjectType()
    {
        return IDBI.class;
    }

    /**
     * See org.springframework.beans.factory.FactoryBean#isSingleton
     *
     * @return false
     */
    public boolean isSingleton()
    {
        return true;
    }

    /**
     * The datasource, which should be managed by spring's transaction system, from which
     * the IDBI will obtain connections
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
    public void afterPropertiesSet() throws Exception
    {
        if (dataSource == null) {
            throw new IllegalStateException("'dataSource' property must be set");
        }
    }
}
