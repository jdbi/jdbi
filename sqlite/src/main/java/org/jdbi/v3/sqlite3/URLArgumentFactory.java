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

package org.jdbi.v3.sqlite3;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Build URL objects as strings.
 */
class URLArgumentFactory extends AbstractArgumentFactory<URL> {
    /**
     * Constructs an {@link ArgumentFactory} for type {@code URL}.
     */
    URLArgumentFactory() {
        super(Types.VARCHAR);
    }

    @Override
    protected Argument build(URL url, ConfigRegistry config) {
        // TODO make this not throw an npe anymore by using our core arguments? all our other ootb arguments are null-safe
        return new Argument() {
            @Override
            public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
                statement.setString(position, url.toString());
            }

            @Override
            public String toString() {
                return String.valueOf(url);
            }
        };
    }
}
