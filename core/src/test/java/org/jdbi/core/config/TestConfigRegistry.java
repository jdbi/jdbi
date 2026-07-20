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
package org.jdbi.core.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.core.mapper.RowMapperFactory;
import org.jdbi.core.mapper.RowMappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestConfigRegistry {

    private ConfigRegistry parent;
    private ConfigRegistry child1;
    private ConfigRegistry child2;
    private ConfigRegistry grandchild1;
    private ConfigRegistry grandchild2;

    @BeforeEach
    public void setUp() {
        this.parent = new ConfigRegistry();
        // TestConfig values are immutable, so a change is installed with configure() rather than mutated in place.
        parent.configure(TestConfig.class, c -> c.addList("list1").addSet("set1").addMap("key1", "value1"));
    }

    @Test
    public void testInheritParentValues() {
        validateSingleConfig(parent.get(TestConfig.class));

        child1 = parent.createCopy();
        validateSingleConfig(child1.get(TestConfig.class));

        child2 = parent.createCopy();
        validateSingleConfig(child2.get(TestConfig.class));

        grandchild1 = child1.createCopy();
        validateSingleConfig(grandchild1.get(TestConfig.class));

        grandchild2 = child2.createCopy();
        validateSingleConfig(grandchild2.get(TestConfig.class));
    }

    @Test
    public void testModifyParentAfterCopy() {
        validateSingleConfig(parent.get(TestConfig.class));

        child1 = parent.createCopy();
        validateSingleConfig(child1.get(TestConfig.class));

        parent.configure(TestConfig.class, c -> c.addList("list2").addSet("set2"));

        // createCopy() snapshots the value set, so a later change to the parent does not reach the copy.
        validateDoubleConfig(parent.get(TestConfig.class));
        validateSingleConfig(child1.get(TestConfig.class));

        grandchild1 = child1.createCopy();

        child1.configure(TestConfig.class, c -> c.addList("list2").addSet("set2"));

        validateDoubleConfig(parent.get(TestConfig.class));
        validateDoubleConfig(child1.get(TestConfig.class));
        validateSingleConfig(grandchild1.get(TestConfig.class));
    }

    @Test
    public void testModifyChildAfterCopy() {
        validateSingleConfig(parent.get(TestConfig.class));

        child1 = parent.createCopy();
        validateSingleConfig(child1.get(TestConfig.class));

        child1.configure(TestConfig.class, c -> c.addList("list2").addSet("set2"));

        // installing on the child does not touch the parent's value.
        validateDoubleConfig(child1.get(TestConfig.class));
        validateSingleConfig(parent.get(TestConfig.class));

        grandchild1 = child1.createCopy();

        grandchild1.configure(TestConfig.class, c -> c.addList("list3").addSet("set3"));

        validateTripleConfig(grandchild1.get(TestConfig.class));
        validateDoubleConfig(child1.get(TestConfig.class));
        validateSingleConfig(parent.get(TestConfig.class));
    }

    @Test
    public void testSiblingsAreIndependent() {
        validateSingleConfig(parent.get(TestConfig.class));

        child1 = parent.createCopy();
        validateSingleConfig(child1.get(TestConfig.class));

        child2 = parent.createCopy();
        validateSingleConfig(child2.get(TestConfig.class));

        child1.configure(TestConfig.class, c -> c.addList("list2").addSet("set2"));

        validateDoubleConfig(child1.get(TestConfig.class));
        validateSingleConfig(parent.get(TestConfig.class));
        validateSingleConfig(child2.get(TestConfig.class));
    }


    @Test
    public void testSiblingsInheritChanges() {
        validateSingleConfig(parent.get(TestConfig.class));

        child1 = parent.createCopy();
        validateSingleConfig(child1.get(TestConfig.class));

        parent.configure(TestConfig.class, c -> c.addList("list2").addSet("set2"));

        child2 = parent.createCopy();

        // child1 was copied while the parent held a single value; child2 after the second value was installed.
        validateDoubleConfig(parent.get(TestConfig.class));
        validateSingleConfig(child1.get(TestConfig.class));
        validateDoubleConfig(child2.get(TestConfig.class));

        parent.configure(TestConfig.class, c -> c.addList("list3").addSet("set3"));

        validateTripleConfig(parent.get(TestConfig.class));
        validateSingleConfig(child1.get(TestConfig.class));
        validateDoubleConfig(child2.get(TestConfig.class));
    }

    @Test
    public void testReadAsMemoizesPerRegistry() {
        AtomicInteger builds = new AtomicInteger();
        View first = parent.readAs(View.class, r -> countingView(builds, r));
        View second = parent.readAs(View.class, r -> countingView(builds, r));

        assertThat(second).isSameAs(first);
        assertThat(builds).hasValue(1);
        assertThat(first.registry).isSameAs(parent.asReadOnlyView());
    }

    @Test
    public void testReadAsViewIsNotInheritedByCopies() {
        AtomicInteger builds = new AtomicInteger();
        View parentView = parent.readAs(View.class, r -> countingView(builds, r));

        child1 = parent.createCopy();
        View childView = child1.readAs(View.class, r -> countingView(builds, r));

        assertThat(childView).isNotSameAs(parentView);
        assertThat(childView.registry).isSameAs(child1.asReadOnlyView());
        assertThat(builds).hasValue(2);
        // the parent's view is unchanged and still memoized
        assertThat(parent.readAs(View.class, r -> countingView(builds, r))).isSameAs(parentView);
        assertThat(builds).hasValue(2);
    }

    // A no-op factory; registering it derives a new (immutable) RowMappers value.
    private static final RowMapperFactory FACTORY = (type, config) -> Optional.empty();

    @Test
    public void testUnforkedChildDelegatesReads() {
        child1 = parent.createChild();
        // an un-forked child holds no config of its own; it reads the parent's value by reference
        assertThat(child1.get(RowMappers.class)).isSameAs(parent.get(RowMappers.class));
    }

    @Test
    public void testUnforkedChildIsLiveViewOfParent() {
        child1 = parent.createChild();
        parent.configure(RowMappers.class, r -> r.register(FACTORY));
        // before it forks, the child reflects a later change to the parent
        assertThat(child1.get(RowMappers.class)).isSameAs(parent.get(RowMappers.class));
    }

    @Test
    public void testChildForkOnWriteIsolatesParent() {
        RowMappers before = parent.get(RowMappers.class);

        child1 = parent.createChild();
        child1.configure(RowMappers.class, r -> r.register(FACTORY));

        // the child forked a private value; the parent is untouched
        assertThat(child1.get(RowMappers.class)).isNotSameAs(before);
        assertThat(parent.get(RowMappers.class)).isSameAs(before);
    }

    @Test
    public void testUnforkedChildReusesParentView() {
        AtomicInteger builds = new AtomicInteger();
        View parentView = parent.readAs(View.class, r -> countingView(builds, r));

        child1 = parent.createChild();
        View childView = child1.readAs(View.class, r -> countingView(builds, r));

        // delegated to the parent: same (warm) view, no rebuild, bound to the parent
        assertThat(childView).isSameAs(parentView);
        assertThat(childView.registry).isSameAs(parent.asReadOnlyView());
        assertThat(builds).hasValue(1);
    }

    @Test
    public void testForkedChildBuildsItsOwnView() {
        AtomicInteger builds = new AtomicInteger();
        parent.readAs(View.class, r -> countingView(builds, r));

        child1 = parent.createChild();
        child1.configure(RowMappers.class, r -> r.register(FACTORY)); // forks
        View childView = child1.readAs(View.class, r -> countingView(builds, r));

        assertThat(childView.registry).isSameAs(child1.asReadOnlyView());
        assertThat(builds).hasValue(2);
    }

    @Test
    public void testInstallInvalidatesMemoizedViews() {
        AtomicInteger builds = new AtomicInteger();
        View before = parent.readAs(View.class, r -> countingView(builds, r));

        parent.configure(RowMappers.class, r -> r.register(FACTORY)); // a config change invalidates views

        View after = parent.readAs(View.class, r -> countingView(builds, r));
        assertThat(after).isNotSameAs(before);
        assertThat(builds).hasValue(2);
    }

    private static View countingView(AtomicInteger builds, ConfigView registry) {
        builds.incrementAndGet();
        return new View(registry);
    }

    private static final class View {
        private final ConfigView registry;

        View(ConfigView registry) {
            this.registry = registry;
        }
    }

    private static void validateSingleConfig(TestConfig config) {
        assertThat(config.getList())
                .hasSize(1)
                .containsExactly("list1");
        assertThat(config.getSet())
                .hasSize(1)
                .containsExactlyInAnyOrder("set1");
        assertThat(config.getMap())
                .hasSize(1)
                .containsEntry("key1", "value1");
    }

    private static void validateDoubleConfig(TestConfig config) {
        assertThat(config.getList())
                .hasSize(2)
                .containsExactly("list1", "list2");
        assertThat(config.getSet())
                .hasSize(2)
                .containsExactlyInAnyOrder("set1", "set2");
        assertThat(config.getMap())
                .hasSize(1)
                .containsEntry("key1", "value1");
    }

    private static void validateTripleConfig(TestConfig config) {
        assertThat(config.getList())
                .hasSize(3)
                .containsExactly("list1", "list2", "list3");
        assertThat(config.getSet())
                .hasSize(3)
                .containsExactlyInAnyOrder("set1", "set2", "set3");
        assertThat(config.getMap())
                .hasSize(1)
                .containsEntry("key1", "value1");
    }

    // An immutable JdbiConfig: each "add" wither returns a new instance with an added entry.
    public static class TestConfig implements JdbiConfig<TestConfig> {

        private final List<String> list;
        private final Set<String> set;
        private final Map<String, String> map;

        public TestConfig() {
            this(List.of(), Set.of(), Map.of());
        }

        private TestConfig(List<String> list, Set<String> set, Map<String, String> map) {
            this.list = list;
            this.set = set;
            this.map = map;
        }

        public TestConfig addList(String key) {
            List<String> copy = new ArrayList<>(list);
            copy.add(key);
            return new TestConfig(List.copyOf(copy), set, map);
        }

        public TestConfig addSet(String key) {
            Set<String> copy = new LinkedHashSet<>(set);
            copy.add(key);
            return new TestConfig(list, Set.copyOf(copy), map);
        }

        public TestConfig addMap(String key, String value) {
            Map<String, String> copy = new LinkedHashMap<>(map);
            copy.put(key, value);
            return new TestConfig(list, set, Map.copyOf(copy));
        }

        public Set<String> getSet() {
            return set;
        }

        public List<String> getList() {
            return list;
        }

        public Map<String, String> getMap() {
            return map;
        }
    }
}
