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
package org.jdbi.v3.sqlobject;

import org.jdbi.v3.core.config.JdbiConfig;

import java.util.Objects;

public class DefaultHandlerFactoryConfiguration implements JdbiConfig<DefaultHandlerFactoryConfiguration> {
    private DefaultHandlerFactory defaultHandlerFactory;

    public DefaultHandlerFactoryConfiguration() {
        defaultHandlerFactory = new DefaultMethodHandlerFactory();
    }

    private DefaultHandlerFactoryConfiguration(DefaultHandlerFactoryConfiguration that) {
        defaultHandlerFactory = that.defaultHandlerFactory;
    }

    /**
     * Returns the configured {@link DefaultHandlerFactory}. The default is {@link DefaultMethodHandlerFactory}.
     *
     * @return the configured {@link DefaultHandlerFactory}.
     */
    public DefaultHandlerFactory getDefaultHandlerFactory() {
        return defaultHandlerFactory;
    }

    /**
     * Configures SqlObject to use the given {@link DefaultHandlerFactory}.
     *
     * @param defaultHandlerFactory the new default handler factory.
     * @return this {@link DefaultHandlerFactoryConfiguration}.
     */
    public DefaultHandlerFactoryConfiguration setDefaultHandlerFactory(DefaultHandlerFactory defaultHandlerFactory) {
        this.defaultHandlerFactory = Objects.requireNonNull(defaultHandlerFactory);
        return this;
    }

    @Override
    public DefaultHandlerFactoryConfiguration createCopy() {
        return new DefaultHandlerFactoryConfiguration(this);
    }
}
