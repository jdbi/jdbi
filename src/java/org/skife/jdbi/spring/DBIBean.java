/* Copyright 2004-2005 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.spring;

import org.skife.jdbi.ConnectionFactory;
import org.skife.jdbi.DBI;
import org.skife.jdbi.IDBI;
import org.skife.jdbi.unstable.decorator.HandleDecorator;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Used to provide an {@see org.skife.jdbi.IDBI} instance to a Spring bean factory
 * <p />
 * Configuration might look like:
 * <pre><code>
 * &lt;beans&gt;
 *  &lt;bean id="propertyConfigurer"
 *        class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer"&gt;
 *      &lt;property name="location"&gt;
 *          &lt;value&gt;WEB-INF/jdbc.properties&lt;/value&gt;
 *      &lt;/property&gt;
 *  &lt;/bean&gt;
 *
 *  &lt;bean id="transactionManager"
 *        class="org.springframework.jdbc.datasource.DataSourceTransactionManager"&gt;
 *      &lt;property name="dataSource"&gt;
 *          &lt;ref local="dataSource"/&gt;
 *      &lt;/property&gt;
 *  &lt;/bean&gt;
 *
 *  &lt;bean id="dataSource"
 *        class="org.springframework.jdbc.datasource.DriverManagerDataSource"&gt;
 *      &lt;property name="driverClassName"&gt;
 *          &lt;value&gt;${jdbc.driver}&lt;/value&gt;
 *      &lt;/property&gt;
 *      &lt;property name="url"&gt;
 *          &lt;value&gt;${jdbc.url}&lt;/value&gt;
 *      &lt;/property&gt;
 *  &lt;/bean&gt;
 *
 *  &lt;bean id="dbi" class="org.skife.jdbi.spring.DBIBean"&gt;
 *      &lt;property name="dataSource"&gt;&lt;ref bean="dataSource" /&gt;&lt;/property&gt;
 *  &lt;/bean&gt;
 * &lt;/beans&gt;
 * </code></pre>
 * The only configuration needed for the <code>IDBI</code> instance is the last bean entry,
 * <code>dbi</code>, but the rest sets up a typical local datasource.
 */
public class DBIBean implements FactoryBean, InitializingBean
{
    private DataSource dataSource;
    private HandleDecorator handleDecorator;

    public DataSource getDataSource()
    {
        return dataSource;
    }

    /**
     * Specify the datasource to be used to draw connections from. Any transaction manager
     * managing the datasource will also provide transaction demarcation for the IDBI built.
     * @param dataSource
     */
    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    /**
     * Specify a decorator builder to decorate all handles created by this DBI instance
     */
    public void setHandleDecoratorBuilder(HandleDecorator builder)
    {
        this.handleDecorator = builder;
    }

    public Object getObject() throws Exception
    {
        final DBI dbi = new DBI(new ConnectionFactory()
        {
            public Connection getConnection()
            {
                return DataSourceUtils.getConnection(dataSource);
            }
        });

        if (handleDecorator != null) dbi.setHandleDecorator(handleDecorator);

        return new SpringDBIAdaptor(dbi);
    }

    public Class getObjectType()
    {
        return IDBI.class;
    }

    /**
     * @return true
     */
    public boolean isSingleton()
    {
        return true;
    }

    /**
     * Ensures that a datasource has been set
     * @throws IllegalStateException if no datasource has been set
     */
    public void afterPropertiesSet() throws Exception
    {
        if (dataSource == null)
        {
            throw new IllegalStateException("must set a dataSource property");
        }
    }
}
