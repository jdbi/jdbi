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

import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestGeneratedSqlObjectProvider {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withPlugins(new H2DatabasePlugin(), new SqlObjectPlugin());

    @Test
    public void missingProviderFailsWithExplanation() {
        assertThatThrownBy(() -> h2Extension.getSharedHandle().attach(UnprocessedDao.class))
                .isInstanceOf(UnableToCreateSqlObjectException.class)
                .hasMessageContaining(GeneratedSqlObjectProvider.class.getSimpleName())
                .hasMessageContaining(UnprocessedDao.class.getName())
                .hasMessageContaining("jdbi3-generator");
    }

    @Test
    public void missingProviderFailsOnDemand() {
        assertThatThrownBy(() -> h2Extension.getJdbi().onDemand(UnprocessedDao.class))
                .isInstanceOf(UnableToCreateSqlObjectException.class);
    }

    // this module does not run the generator, so no provider exists for this type
    @GenerateSqlObject
    public interface UnprocessedDao {
        @SqlQuery("select 42")
        int answer();
    }
}
