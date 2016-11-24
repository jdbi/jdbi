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
package org.jdbi.v3.core.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jdbi.v3.core.JdbiConfig;
import org.jdbi.v3.core.util.bean.CaseInsensitiveColumnNameStrategy;
import org.jdbi.v3.core.util.bean.ColumnNameMappingStrategy;
import org.jdbi.v3.core.util.bean.SnakeCaseColumnNameStrategy;

public class ReflectionMappers implements JdbiConfig<ReflectionMappers> {
    private List<ColumnNameMappingStrategy> columnNameMappingStrategies;

    public ReflectionMappers() {
        columnNameMappingStrategies = Arrays.asList(
                CaseInsensitiveColumnNameStrategy.INSTANCE,
                SnakeCaseColumnNameStrategy.INSTANCE);
    }

    public List<ColumnNameMappingStrategy> getColumnNameMappingStrategies() {
        return Collections.unmodifiableList(columnNameMappingStrategies);
    }

    public void setColumnNameMappingStrategies(List<ColumnNameMappingStrategy> columnNameMappingStrategies) {
        this.columnNameMappingStrategies = new ArrayList<>(columnNameMappingStrategies);
    }

    @Override
    public ReflectionMappers createChild() {
        return null;
    }
}
