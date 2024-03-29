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
package org.jdbi.v3.moshi;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.json.JsonConfig;
import org.jdbi.v3.json.JsonPlugin;

/**
 * Moshi integration plugin.
 * <p>
 * Adds support for {@code @Json} qualifying annotation via {@link com.squareup.moshi.Moshi}.
 *
 * @see org.jdbi.v3.json.Json
 */
public class MoshiPlugin extends JdbiPlugin.Singleton {

    @Override
    public void customizeJdbi(Jdbi jdbi) {
        jdbi.installPlugin(new JsonPlugin());
        jdbi.getConfig(JsonConfig.class).setJsonMapper(new MoshiJsonMapper());
    }
}
