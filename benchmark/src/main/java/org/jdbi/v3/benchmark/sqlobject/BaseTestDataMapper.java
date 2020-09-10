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
package org.jdbi.v3.benchmark.sqlobject;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BaseTestDataMapper {
    protected TestData mapInternal(final ResultSet r) throws SQLException {
        final long id = r.getLong(1);
        final String name = r.getString(2);
        final String description = r.getString(3);

        final TestContent content = new TestContent(name, description);

        return new TestData(id, content);
    }
}
