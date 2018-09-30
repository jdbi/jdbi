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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * Utility class which constructs an {@link Jdbi} instance which can conveniently participate
 * in Spring's transaction management system.
 */
public class JdbiFactoryBean implements FactoryBean<Jdbi> {
    private DataSource dataSource;
    private final Map<String, Object> globalDefines = new HashMap<>();

    private boolean autoInstallPlugins = false;
    private Collection<JdbiPlugin> plugins = Collections.emptyList();
    private Collection<RowMapper<?>> rowMappers = Collections.emptyList();
    private Collection<ColumnMapper<?>> columnMappers = Collections.emptyList();

    public JdbiFactoryBean() {}

    public JdbiFactoryBean(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * See {@link org.springframework.beans.factory.FactoryBean#getObject}
     */
    @Override
    public Jdbi getObject() throws Exception {
        final Jdbi jdbi = Jdbi.create(() -> DataSourceUtils.getConnection(dataSource));

        if (autoInstallPlugins) {
            jdbi.installPlugins();
        }

        plugins.forEach(jdbi::installPlugin);
        rowMappers.forEach(jdbi::registerRowMapper);
        columnMappers.forEach(jdbi::registerColumnMapper);

        globalDefines.forEach(jdbi::define);

        return jdbi;
    }

    /**
     * See {@link org.springframework.beans.factory.FactoryBean#getObjectType}
     */
    @Override
    public Class<Jdbi> getObjectType() {
        return Jdbi.class;
    }

    /**
     * See {@link org.springframework.beans.factory.FactoryBean#isSingleton}
     *
     * @return false
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * The datasource, which should be managed by spring's transaction system, from which
     * the {@link Jdbi} will obtain connections
     *
     * @param dataSource the data source.
     * @return this
     */
    public JdbiFactoryBean setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    /**
     * Installs the given plugins which will be installed into the {@link Jdbi}.
     * @param plugins collection of Jdbi plugins to install.
     * @return this
     */
    @Autowired(required = false)
    public JdbiFactoryBean setPlugins(Collection<JdbiPlugin> plugins) {
        this.plugins = new ArrayList<>(plugins);
        return this;
    }

    /**
     * Installs the given {@link RowMapper}s which will be installed into the {@link Jdbi}.
     * @param mappers collection of Jdbi mappers to install.
     * @return this
     */
    @Autowired(required = false)
    public JdbiFactoryBean setRowMappers(Collection<RowMapper<?>> mappers) {
        this.rowMappers = new ArrayList<>(mappers);
        return this;
    }

    /**
     * Installs the given {@link ColumnMapper}s which will be installed into the {@link Jdbi}.
     * @param mappers collection of Jdbi mappers to install.
     * @return this
     */
    @Autowired(required = false)
    public JdbiFactoryBean setColumnMappers(Collection<ColumnMapper<?>> mappers) {
        this.columnMappers = new ArrayList<>(mappers);
        return this;
    }

    /**
     * Sets whether to install plugins automatically from the classpath, using
     * {@link java.util.ServiceLoader} manifests.
     *
     * @param autoInstallPlugins whether to install plugins automatically from
     *                           the classpath.
     * @return this
     * @see Jdbi#installPlugins() for detail
     */
    public JdbiFactoryBean setAutoInstallPlugins(boolean autoInstallPlugins) {
        this.autoInstallPlugins = autoInstallPlugins;
        return this;
    }

    public void setGlobalDefines(Map<String, Object> defines) {
        globalDefines.putAll(defines);
    }

    /**
     * Verifies that a dataSource has been set
     */
    @PostConstruct
    @SuppressWarnings("unused")
    private void afterPropertiesSet() {
        if (dataSource == null) {
            throw new IllegalStateException("'dataSource' property must be set");
        }
    }
}
