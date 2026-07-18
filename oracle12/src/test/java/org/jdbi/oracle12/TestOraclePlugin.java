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

import org.jdbi.core.ConnectionFactory;
import org.jdbi.core.Jdbi;
import org.jdbi.core.argument.Arguments;
import org.jdbi.core.argument.NullArgument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestOraclePlugin {

    @Test
    public void testPluginSetsUntypedNullArgument() {
        ConnectionFactory connectionFactory = () -> {
            throw new UnsupportedOperationException();
        };

        // without the plugin, the default is Types.OTHER
        Jdbi defaultJdbi = Jdbi.builder(connectionFactory).build();
        NullArgument defaultNull = (NullArgument) defaultJdbi.getConfig(Arguments.class).getUntypedNullArgument();
        assertThat(defaultNull.getSqlType()).isEqualTo(Types.OTHER);

        // with the plugin installed, the untyped null uses Types.NULL
        Jdbi oracleJdbi = Jdbi.builder(connectionFactory).installPlugin(new OraclePlugin()).build();
        NullArgument oracleNull = (NullArgument) oracleJdbi.getConfig(Arguments.class).getUntypedNullArgument();
        assertThat(oracleNull.getSqlType()).isEqualTo(Types.NULL);
    }

    @Test
    public void testPluginIsSingleton() {
        OraclePlugin plugin1 = new OraclePlugin();
        OraclePlugin plugin2 = new OraclePlugin();
        assertThat(plugin1).isEqualTo(plugin2)
                .hasSameHashCodeAs(plugin2);
    }
}
