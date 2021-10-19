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
package org.jdbi.v3.core.statement;

import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Locale;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSqlMetaData {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    @Test
    public void testQueryCatalogs() {
        Handle h = h2Extension.openHandle();

        List<String> catalogNames = h.queryMetadata(DatabaseMetaData::getCatalogs)
            .mapTo(String.class)
            .list();

        assertThat(catalogNames).hasSize(1);
        String catalog = catalogNames.iterator().next().toLowerCase(Locale.ROOT);

        String dbConnectionString = h2Extension.getUri().toLowerCase(Locale.ROOT);

        assertThat(dbConnectionString).endsWith(catalog);

    }

    @Test
    public void testSimple() {
        Handle h = h2Extension.openHandle();

        String url = h.queryMetadata(DatabaseMetaData::getURL);
        String dbConnectionString = h2Extension.getUri().toLowerCase(Locale.ROOT);

        assertThat(dbConnectionString).isEqualTo(url);
    }

    @Test
    public void testTransactions() {
        Handle h = h2Extension.openHandle();

        boolean supportsTransactions = h.queryMetadata(DatabaseMetaData::supportsTransactions);

        assertThat(supportsTransactions).isTrue();
    }
}
