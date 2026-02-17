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
package org.jdbi.spring;

import org.jdbi.core.Jdbi;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;

public class JdbiRepositoryFactoryBean implements FactoryBean<Object>, ApplicationContextAware, BeanFactoryAware, InitializingBean {

    private Class<?> objectType;
    private String jdbiQualifier;
    private BeanFactory beanFactory;

    /**
     * Returns the {@link Jdbi} instance to attach this repository to.
     *
     * @return The {@link Jdbi} instance to attach this repository to.
     */
    protected Jdbi getJdbi() {
        if (jdbiQualifier != null) {
            return beanFactory.getBean(jdbiQualifier, Jdbi.class);
        } else {
            return beanFactory.getBean(Jdbi.class);
        }
    }

    @Override
    public Object getObject() {
        return JdbiJtaBinder.bind(getJdbi(), objectType);
    }

    @Override
    public Class<?> getObjectType() {
        return objectType;
    }

    /**
     * The object type of the repository.
     */
    @SuppressWarnings("unused")
    public void setObjectType(Class<?> objectType) {
        this.objectType = objectType;
    }

    /**
     * Set the jdbi qualifier.
     * @param jdbiQualifier The name of the jdbi bean to bind the repository to.
     *                      if <code>null</code> then no name will be specified during resolution.
     */
    @SuppressWarnings("unused")
    public void setJdbiQualifier(@Nullable String jdbiQualifier) {
        this.jdbiQualifier = jdbiQualifier;
    }

    /**
     * Verifies that the object type has been set
     */
    @Override
    public void afterPropertiesSet() {
        if (objectType == null) {
            throw new IllegalStateException("'type' property must be set");
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        beanFactory = context;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
}
