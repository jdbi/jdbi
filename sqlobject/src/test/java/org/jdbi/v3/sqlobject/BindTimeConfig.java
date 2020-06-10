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

package org.jdbi.v3.sqlobject;

import java.time.Clock;

import org.jdbi.v3.core.config.JdbiConfig;

public class BindTimeConfig implements JdbiConfig<BindTimeConfig> {
    private Clock clock;

    Clock getClock() {
        return clock;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public BindTimeConfig createCopy() {
        BindTimeConfig copy = new BindTimeConfig();
        copy.clock = this.clock;
        return copy;
    }
}
