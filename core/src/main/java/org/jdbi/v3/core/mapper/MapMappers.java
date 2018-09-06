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

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.meta.Beta;

@Beta
public class MapMappers implements JdbiConfig<MapMappers> {

    private CaseStrategy caseChange;

    public MapMappers() {
        caseChange = CaseStrategy.LOCALE_LOWER;
    }

    private MapMappers(MapMappers that) {
        caseChange = that.caseChange;
    }

    @Beta
    public CaseStrategy getCaseChange() {
        return caseChange;
    }

    @Beta
    public MapMappers setCaseChange(CaseStrategy caseChange) {
        this.caseChange = caseChange;
        return this;
    }

    @Override
    public MapMappers createCopy() {
        return new MapMappers(this);
    }
}
