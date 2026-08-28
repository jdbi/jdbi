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
package org.jdbi.v3.core.config;

import org.jdbi.v3.core.config.TestConfigRegistry.TestConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the timing of {@link ConfigRegistry#createCopy()}: by default a copy materializes each config
 * object on first access, while {@link ConfigRegistry#setEagerCopies(boolean)} restores the
 * copy-at-creation timing of releases before 3.55.0.
 */
public class TestConfigRegistryCopyTiming {

    @Test
    public void lazyCopySeesSourceChangeBeforeFirstAccess() {
        ConfigRegistry parent = new ConfigRegistry();
        parent.get(TestConfig.class).addList("list1");

        ConfigRegistry copy = parent.createCopy();
        parent.get(TestConfig.class).addList("list2");

        assertThat(copy.get(TestConfig.class).getList()).containsExactly("list1", "list2");
    }

    @Test
    public void lazyCopyIsIsolatedAfterFirstAccess() {
        ConfigRegistry parent = new ConfigRegistry();
        parent.get(TestConfig.class).addList("list1");

        ConfigRegistry copy = parent.createCopy();
        TestConfig copyConfig = copy.get(TestConfig.class);

        parent.get(TestConfig.class).addList("list2");
        copyConfig.addList("list3");

        assertThat(copyConfig.getList()).containsExactly("list1", "list3");
        assertThat(parent.get(TestConfig.class).getList()).containsExactly("list1", "list2");
    }

    @Test
    public void configCreatedThroughLazyCopyIsIsolated() {
        ConfigRegistry parent = new ConfigRegistry();

        ConfigRegistry copy = parent.createCopy();
        copy.get(TestConfig.class).addList("copy-only");

        assertThat(parent.get(TestConfig.class).getList()).isEmpty();
    }

    @Test
    public void eagerCopyTakesSnapshotAtCopyTime() {
        ConfigRegistry parent = new ConfigRegistry();
        parent.setEagerCopies(true);
        parent.get(TestConfig.class).addList("list1");

        ConfigRegistry copy = parent.createCopy();
        parent.get(TestConfig.class).addList("list2");

        assertThat(copy.get(TestConfig.class).getList()).containsExactly("list1");
        assertThat(parent.get(TestConfig.class).getList()).containsExactly("list1", "list2");
    }

    @Test
    public void copiesInheritEagerMode() {
        ConfigRegistry parent = new ConfigRegistry();
        parent.setEagerCopies(true);

        ConfigRegistry copy = parent.createCopy();
        assertThat(copy.isEagerCopies()).isTrue();

        ConfigRegistry grandchild = copy.createCopy();
        assertThat(grandchild.isEagerCopies()).isTrue();
    }

    @Test
    public void eagerCopyOfLazyCopyIncludesUnmaterializedConfigs() {
        ConfigRegistry root = new ConfigRegistry();
        root.get(TestConfig.class).addList("list1");

        ConfigRegistry lazy = root.createCopy();
        lazy.setEagerCopies(true);
        ConfigRegistry eager = lazy.createCopy();

        root.get(TestConfig.class).addList("list2");

        assertThat(eager.get(TestConfig.class).getList()).containsExactly("list1");
    }
}
