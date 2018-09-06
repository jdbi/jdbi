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
package org.jdbi.v3.spring4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * Utility class which constructs an {@link Jdbi} instance which can conveniently
 * participate in Spring's transaction management system.
 */
public class JdbiFactoryBean extends AbstractFactoryBean<Jdbi> {
    private DataSource dataSource;
    private final Map<String, Object> globalDefines = new HashMap<>();

    private boolean autoInstallPlugins = false;
    private Collection<JdbiPlugin> plugins = Collections.emptyList();

    public JdbiFactoryBean() {}

    public JdbiFactoryBean(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected Jdbi createInstance() throws Exception {
        final Jdbi jdbi = Jdbi.create(() -> DataSourceUtils.getConnection(dataSource));

        if (autoInstallPlugins) {
            jdbi.installPlugins();
        }

        plugins.forEach(jdbi::installPlugin);

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
    @SuppressWarnings("unused")
    public void afterPropertiesSet() throws Exception {
        if (dataSource == null) {
            throw new IllegalStateException("'dataSource' property must be set");
        }
        super.afterPropertiesSet();
    }
}
