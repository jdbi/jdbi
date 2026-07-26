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
package org.jdbi.spring.jta;

import org.jdbi.core.Handles;
import org.jdbi.core.Jdbi;
import org.jdbi.core.spi.JdbiPlugin;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.testing.internal.JdbiLeakChecker;

/**
 * Attaches a {@link JdbiLeakChecker} to a {@link Jdbi} at assembly time, so a Spring-managed (immutable) {@code Jdbi}
 * can participate in leak checking.
 */
public class JdbiLeakCheckerPlugin implements JdbiPlugin {

    private final JdbiLeakChecker leakChecker;

    public JdbiLeakCheckerPlugin(JdbiLeakChecker leakChecker) {
        this.leakChecker = leakChecker;
    }

    @Override
    public void configure(Jdbi.Builder builder) {
        builder.configure(Handles.class, h -> h.addListener(leakChecker));
        builder.configure(SqlStatements.class, s -> s.addContextListener(leakChecker));
    }
}
