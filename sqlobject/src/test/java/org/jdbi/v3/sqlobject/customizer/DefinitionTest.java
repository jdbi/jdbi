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
package org.jdbi.v3.sqlobject.customizer;

import org.jdbi.v3.core.statement.DefinedAttributeTemplateEngine;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.UseTemplateEngine;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class DefinitionTest {

    @RegisterExtension
    public static final JdbiExtension DB = JdbiExtension.h2()
            .withPlugin(new SqlObjectPlugin());

    @Test
    void definition() {
        final var defnDao = DB.getSharedHandle().attach(DefinitionDao.class);
        assertThat(defnDao.select()).isEqualTo(42);
    }

    @UseTemplateEngine(DefinedAttributeTemplateEngine.class)
    @Definition(key = "TYPE_DEFN", value = "2")
    @Definition(key = "TYPE_DEFN_2", value = "1")
    public interface DefinitionDao extends SuperDefinition1, SuperDefinition2 {
        @Definition
        static String staticMethod() {
            return "sel";
        }

        @SqlQuery("<staticMethod><superStaticMethod> <STATIC_CONSTANT> + <TYPE_DEFN> + <TYPE_DEFN_2> + <SUPER_TYPE_DEFN>")
        int select();
    }

    @SuppressWarnings("checkstyle:InterfaceIsType")
    public interface SuperDefinition1 {
        @Definition
        int STATIC_CONSTANT = 36;
    }

    @Definition(key = "SUPER_TYPE_DEFN", value = "3")
    public interface SuperDefinition2 {
        @Definition
        static String superStaticMethod() {
            return "ect";
        }
    }
}
