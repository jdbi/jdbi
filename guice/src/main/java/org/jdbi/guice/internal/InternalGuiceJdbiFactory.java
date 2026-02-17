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
package org.jdbi.guice.internal;

import java.util.Set;

import javax.sql.DataSource;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.jdbi.core.Jdbi;
import org.jdbi.guice.GuiceJdbiCustomizer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * JDBI Factory creates a new {@link Jdbi} instance.
 */
@Singleton
public final class InternalGuiceJdbiFactory implements Provider<Jdbi> {

    private final DataSource dataSource;
    // this is a set because it contains at least two customizers; one for the module and the global customizer (which may be a dummy).
    private final Set<GuiceJdbiCustomizer> moduleCustomizers;

    @Inject
    InternalGuiceJdbiFactory(final DataSource dataSource,
        @JdbiInternal final Set<GuiceJdbiCustomizer> moduleCustomizers) {
        this.dataSource = checkNotNull(dataSource, "dataSource is null");
        this.moduleCustomizers = checkNotNull(moduleCustomizers, "moduleCustomizers is null");
    }

    @Override
    public Jdbi get() {
        Jdbi jdbi = Jdbi.create(dataSource);
        moduleCustomizers.forEach(c -> c.customize(jdbi));

        return jdbi;
    }
}
