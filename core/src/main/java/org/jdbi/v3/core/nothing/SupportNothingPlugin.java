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
package org.jdbi.v3.core.nothing;

import java.util.function.Function;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.Configurable;
import org.jdbi.v3.core.spi.JdbiPlugin;

public class SupportNothingPlugin extends SupportNothingBase implements JdbiPlugin {
    public SupportNothingPlugin() {
        this((clazz) -> new UnsupportedOperationException("You've installed " + clazz.getSimpleName() + " — operation denied"));
    }

    public SupportNothingPlugin(Function<Class<?>, ? extends RuntimeException> provider) {
        super(provider);
    }

    @Override
    public void customizeJdbi(Jdbi jdbi) {
        configure(jdbi);
    }

    @Override
    public Handle customizeHandle(Handle handle) {
        return configure(handle);
    }

    private <T extends Configurable<T>> T configure(T configurable) {
        configurable.registerArgument(new SupportNothingArgumentFactory(super.provider));
        configurable.registerRowMapper(new SupportNothingRowMapperFactory(super.provider));
        configurable.registerColumnMapper(new SupportNothingColumnMapperFactory(super.provider));

        return configurable;
    }
}
