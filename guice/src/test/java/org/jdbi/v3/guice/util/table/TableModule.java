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
package org.jdbi.v3.guice.util.table;

import java.lang.annotation.Annotation;

import com.google.inject.Provides;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.guice.AbstractJdbiDefinitionModule;

public class TableModule extends AbstractJdbiDefinitionModule {

    private final String tableName;

    public TableModule(Annotation a, String tableName) {
        super(a);
        this.tableName = tableName;
    }

    public TableModule(Class<? extends Annotation> a, String tableName) {
        super(a);
        this.tableName = tableName;
    }

    public void configureJdbi() {
        exposeBinding(TableDao.class);
    }

    @Provides
    TableDao getDao(Jdbi jdbi) {
        return TableDao.getDao(jdbi, tableName);
    }
}
