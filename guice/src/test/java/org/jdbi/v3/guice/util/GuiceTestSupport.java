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
package org.jdbi.v3.guice.util;

import javax.sql.DataSource;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;
import org.jdbi.v3.core.Jdbi;

public final class GuiceTestSupport {

    private GuiceTestSupport() {
        throw new AssertionError("GuiceTestSupport can not be instantiated");
    }

    public static Injector createTestInjector(Module... modules) {

        ImmutableSet.Builder<Module> moduleBuilder = ImmutableSet.builder();
        moduleBuilder.add(modules);
        moduleBuilder.add(
            Modules.disableCircularProxiesModule(),
            Modules.requireExplicitBindingsModule(),
            Modules.requireExactBindingAnnotationsModule(),
            Modules.requireAtInjectOnConstructorsModule());

        return Guice.createInjector(Stage.PRODUCTION, moduleBuilder.build());
    }

    public static void executeSql(DataSource datasource, String... sqlStatements) {
        Jdbi jdbi = Jdbi.create(datasource);
        jdbi.inTransaction(h -> {
            for (String sql : sqlStatements) {
                h.execute(sql);
            }
            return null;
        });
    }
}
