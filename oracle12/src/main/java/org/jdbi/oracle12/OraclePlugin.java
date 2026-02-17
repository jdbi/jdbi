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
package org.jdbi.oracle12;

import java.sql.Types;

import org.jdbi.core.Jdbi;
import org.jdbi.core.argument.Arguments;
import org.jdbi.core.argument.NullArgument;
import org.jdbi.core.spi.JdbiPlugin;

/**
 * Jdbi plugin for Oracle databases.
 *
 * <p>Oracle does not support {@link Types#OTHER} for untyped null values,
 * which is the default in Jdbi. This plugin configures the untyped null
 * argument to use {@link Types#NULL} instead, which Oracle accepts.</p>
 *
 * <p>Install this plugin when using Jdbi with an Oracle database to avoid
 * errors when binding null values without an explicit type.</p>
 */
public class OraclePlugin extends JdbiPlugin.Singleton {

    @Override
    public void customizeJdbi(Jdbi jdbi) {
        jdbi.configure(Arguments.class, arguments ->
                arguments.setUntypedNullArgument(new NullArgument(Types.NULL)));
    }
}
