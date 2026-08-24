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
package org.jdbi.v3.stringtemplate4;

import java.util.List;

import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.BindMethodsList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.customizer.DefineNamedBindings;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestBindMethodsListWithStringTemplate {
    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
        .withPlugin(new SqlObjectPlugin());

    public record Boo(String a, String b) {}

    public interface Dao {
        @UseStringTemplateEngine
        @DefineNamedBindings
        @SqlQuery("SELECT <if(flag)>('c', 'd') IN (<list>)<else>TRUE<endif>")
        boolean test2(@Define boolean flag,
            @BindMethodsList(methodNames = { "a", "b" }) List<Boo> list);
    }

    @Test
    public void reproduceIssue() {
        Dao dao = h2Extension.getJdbi().onDemand(Dao.class);

        List<Boo> list = List.of(
            new Boo("c", "d"),
            new Boo("e", "f")
        );

        assertTrue(dao.test2(true, list));
    }
}
