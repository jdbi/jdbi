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
package org.jdbi.sqlobject;

import org.jdbi.core.Jdbi;
import org.jdbi.core.internal.OnDemandExtensions;
import org.jdbi.core.spi.JdbiPlugin;

/**
 * Plugin that installs the SqlObject extension.
 */
public class SqlObjectPlugin extends JdbiPlugin.Singleton {
    @Override
    public void configure(Jdbi.Builder builder) {

        // support for generated classes (jdbi-generator)
        GeneratorSqlObjectFactory generatorSqlObjectFactory = new GeneratorSqlObjectFactory();
        builder.registerExtension(generatorSqlObjectFactory);
        builder.configure(OnDemandExtensions.class, c -> c.factory(generatorSqlObjectFactory));

        // register SQL object proxy factory
        builder.registerExtension(new SqlObjectFactory());
    }
}
