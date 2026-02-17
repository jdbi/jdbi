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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestOraclePlugin {

    @Test
    public void testPluginSetsUntypedNullArgument() {
        Jdbi jdbi = Jdbi.create(() -> {
            throw new UnsupportedOperationException();
        });

        // before installing the plugin, the default is Types.OTHER
        NullArgument defaultNull = (NullArgument) jdbi.getConfig(Arguments.class).getUntypedNullArgument();
        assertThat(defaultNull.getSqlType()).isEqualTo(Types.OTHER);

        jdbi.installPlugin(new OraclePlugin());

        // after installing the plugin, the untyped null uses Types.NULL
        NullArgument oracleNull = (NullArgument) jdbi.getConfig(Arguments.class).getUntypedNullArgument();
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
