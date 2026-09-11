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
package org.jdbi.v3.core.result;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.meta.Beta;

import static java.util.Objects.requireNonNull;

/**
 * A {@link RowReducer} that reduces the rows of a join query into a stream of root objects and links the
 * joined objects to their root. One type is one table: every object is identified by its type and its key
 * column, so a table row is one instance no matter through how many relations the query reaches it.
 *
 * <p>Columns are named relative to the relation, in the style of {@code @Nested} mappers. The root type reads
 * its columns without a prefix. A joined type reads them under the relation prefix and an underscore, and
 * nested relations compound: {@code id}, {@code author_id}, {@code author_award_id}. The key column is
 * {@code id} unless {@link #of(Class, String)} names another. A relation is present on a path when the
 * result set has its key column there, so select the root's own foreign key columns under another label
 * than a relation key, or not at all: with {@code SELECT b.*} and a relation {@code category}, the book's
 * {@code category_id} column alone would produce a {@code Category} that has only its key.
 * Keys are compared by value: integral keys of any JDBC type are equal when their values are, decimals
 * ignore trailing zeros, and {@code byte[]} keys are compared by content.
 *
 * <pre>{@code
 * List<Book> books = handle.createQuery(
 *         "SELECT b.id, b.title, a.id author_id, a.name author_name, e.id editor_id, e.name editor_name "
 *             + "FROM books b JOIN persons a ON a.id = b.author_id JOIN persons e ON e.id = b.editor_id")
 *     .reduceRows(JoinRowReducer.of(Book.class).mappedBy(BeanMapper::of)
 *         .one("author", Person.class, Book::setAuthor)
 *         .one("editor", Person.class, Book::setEditor))
 *     .toList();
 * }</pre>
 *
 * <p>{@link #one(String, Class, BiConsumer) one} declares a to-one relation and {@link #many(String, Class,
 * BiConsumer) many} a to-many relation, both over inner and outer joins: a {@code NULL} key produces no object
 * for that row. A joined type can have relations of its own, declared on a nested reducer that is passed in
 * place of the class. A class that is declared anywhere in the reducer, the root type included, refers to that
 * declaration, so the author and the editor above are one {@code Person} instance when they are the same
 * person, and a self join is {@code of(Category.class).one("parent", Category.class, Category::setParent)}
 * with the parent being the same instance as the root category with that key. The recursion of a self join
 * ends where the selected columns end: no {@code parent_parent_id} column, no grandparent.
 *
 * <p>Mappers come from the registry by type and prefix, see {@link RowView#getRow(Class, String)}, so
 * register one prefixed mapper per relation path that the query selects, the root under the empty prefix
 * that a mapper registered without a prefix declares. Or pass a mapper factory such as {@code BeanMapper::of}
 * to {@link #mappedBy(BiFunction)} and register nothing. A factory on the root applies to every type that has
 * none of its own, and a factory is also the way to use a custom row mapper for a type.
 *
 * <p>Linkers run once per distinct pair after the last row, and the relations of a joined type are linked
 * before the joined type is linked to its parent, so a linker receives a complete object unless the graph
 * is recursive. If the query selects a type on several paths, the object is mapped from the shallowest
 * path, and among paths of one depth from the one laid out first, breadth first in declaration order, so
 * the result does not depend on row order.
 * Root objects are returned in the order of their first row, and only keys that appear in the root key
 * column become roots. A reduction fails when the result set has no root key column, when a declared
 * relation has its key column on no path, when a root mapper returns null, or when a {@code one} relation
 * sees a second, different key for the same parent.
 *
 * <p>Instances are immutable. Each call to {@code one}, {@code many} or {@code mappedBy} returns a new
 * reducer, and all mutable state lives in the container of one reduction, so a reducer can be shared between
 * statements and threads. The protected copy constructor gives a subclass the public no-arg constructor that
 * {@code @UseRowReducer} requires.
 *
 * @param <R> the root type, and the result element type
 */
@Beta
public class JoinRowReducer<R> implements RowReducer<JoinRowReducer.Container<R>, R> {
    private static final String DEFAULT_KEY_COLUMN = "id";

    private final Class<R> type;
    private final String keyColumn;
    private final BiFunction<Class<?>, String, ? extends RowMapper<?>> mapperFactory;
    private final List<Link<R, ?>> links;

    private JoinRowReducer(Class<R> type, String keyColumn, BiFunction<Class<?>, String, ? extends RowMapper<?>> mapperFactory, List<Link<R, ?>> links) {
        this.type = type;
        this.keyColumn = keyColumn;
        this.mapperFactory = mapperFactory;
        this.links = links;
    }

    /**
     * Copies the configuration of another reducer. Use this constructor to give a reducer a public no-arg
     * constructor, as required by {@code @UseRowReducer} in the sqlobject module:
     *
     * <pre>{@code
     * public class ContactReducer extends JoinRowReducer<Contact> {
     *     public ContactReducer() {
     *         super(JoinRowReducer.of(Contact.class).many("phone", Phone.class, Contact::addPhone));
     *     }
     * }
     * }</pre>
     *
     * @param prototype the reducer to copy
     */
    protected JoinRowReducer(JoinRowReducer<R> prototype) {
        this(prototype.type, prototype.keyColumn, prototype.mapperFactory, prototype.links);
    }

    /**
     * Creates a reducer for a type whose key column is {@code id}.
     *
     * @param type the type, one per table
     * @param <R> the type
     * @return a reducer that produces one object per distinct key
     */
    public static <R> JoinRowReducer<R> of(Class<R> type) {
        return of(type, DEFAULT_KEY_COLUMN);
    }

    /**
     * Creates a reducer for a type with the given key column.
     *
     * @param type the type, one per table
     * @param keyColumn the column that identifies a row of the table, relative to the relation prefix
     * @param <R> the type
     * @return a reducer that produces one object per distinct key
     */
    public static <R> JoinRowReducer<R> of(Class<R> type, String keyColumn) {
        return new JoinRowReducer<>(requireNonNull(type, "type"), requireNonNull(keyColumn, "keyColumn"), null, List.of());
    }

    /**
     * Uses the given factory to create the mapper for a type and column prefix, for example
     * {@code BeanMapper::of}, instead of the registered mapper. On the root reducer, the factory also
     * applies to every type in the reducer that has no factory of its own.
     *
     * <pre>{@code
     * JoinRowReducer.of(Book.class)
     *     .mappedBy((type, prefix) -> type == Book.class ? bookMapper : BeanMapper.of(type, prefix))
     * }</pre>
     *
     * @param mapperFactory creates the mapper for a type and a prefix, the empty prefix for the root
     * @return a new reducer that maps with the factory
     */
    public JoinRowReducer<R> mappedBy(BiFunction<Class<?>, String, ? extends RowMapper<?>> mapperFactory) {
        return new JoinRowReducer<>(type, keyColumn, requireNonNull(mapperFactory, "mapperFactory"), links);
    }

    /**
     * Adds a to-one relation to a type that is declared elsewhere in the reducer, or that has no relations.
     * A type that is declared nowhere gets the key column {@code id}; pass {@code of(type, keyColumn)} for
     * another.
     *
     * @param prefix the column prefix of the relation, for example {@code author} for {@code author_id}
     * @param joinedType the joined type
     * @param linker sets the joined object on the parent, for example {@code Book::setAuthor}
     * @param <J> the joined type
     * @return a new reducer that also links the joined object
     */
    public <J> JoinRowReducer<R> one(String prefix, Class<J> joinedType, BiConsumer<? super R, ? super J> linker) {
        return withLink(new Link<>(prefix, joinedType, null, linker, false));
    }

    /**
     * Adds a to-one relation to a type with relations of its own.
     *
     * @param prefix the column prefix of the relation
     * @param joined the reducer for the joined type, which declares its relations
     * @param linker sets the joined object on the parent
     * @param <J> the joined type
     * @return a new reducer that also links the joined object
     */
    public <J> JoinRowReducer<R> one(String prefix, JoinRowReducer<J> joined, BiConsumer<? super R, ? super J> linker) {
        return withLink(new Link<>(prefix, requireNonNull(joined, "joined").type, joined, linker, false));
    }

    /**
     * Adds a to-many relation to a type that is declared elsewhere in the reducer, or that has no relations.
     * A type that is declared nowhere gets the key column {@code id}; pass {@code of(type, keyColumn)} for
     * another.
     *
     * @param prefix the column prefix of the relation, for example {@code phone} for {@code phone_id}
     * @param joinedType the joined type
     * @param linker adds the joined object to the parent, for example {@code Contact::addPhone}
     * @param <J> the joined type
     * @return a new reducer that also links the joined objects
     */
    public <J> JoinRowReducer<R> many(String prefix, Class<J> joinedType, BiConsumer<? super R, ? super J> linker) {
        return withLink(new Link<>(prefix, joinedType, null, linker, true));
    }

    /**
     * Adds a to-many relation to a type with relations of its own.
     *
     * @param prefix the column prefix of the relation
     * @param joined the reducer for the joined type, which declares its relations
     * @param linker adds the joined object to the parent
     * @param <J> the joined type
     * @return a new reducer that also links the joined objects
     */
    public <J> JoinRowReducer<R> many(String prefix, JoinRowReducer<J> joined, BiConsumer<? super R, ? super J> linker) {
        return withLink(new Link<>(prefix, requireNonNull(joined, "joined").type, joined, linker, true));
    }

    private JoinRowReducer<R> withLink(Link<R, ?> link) {
        List<Link<R, ?>> newLinks = new ArrayList<>(links.size() + 1);
        newLinks.addAll(links);
        newLinks.add(link);
        JoinRowReducer<R> result = new JoinRowReducer<>(type, keyColumn, mapperFactory, List.copyOf(newLinks));
        result.declaredNodes();
        return result;
    }

    /**
     * Collects the reducers of the graph by type and fails if two different reducers declare the same type.
     */
    private Map<Class<?>, JoinRowReducer<?>> declaredNodes() {
        Map<Class<?>, JoinRowReducer<?>> nodes = new HashMap<>();
        collectDeclaredNodes(nodes, new IdentityHashMap<>());
        return nodes;
    }

    private void collectDeclaredNodes(Map<Class<?>, JoinRowReducer<?>> nodes, Map<JoinRowReducer<?>, Boolean> visited) {
        if (visited.put(this, Boolean.TRUE) != null) {
            return;
        }
        if (nodes.putIfAbsent(type, this) != null) {
            throw new IllegalArgumentException("Two different reducers declare " + type.getName()
                + ". Declare its relations once and refer to the type by class elsewhere");
        }
        for (Link<R, ?> link : links) {
            if (link.joined != null) {
                link.joined.collectDeclaredNodes(nodes, visited);
            }
        }
    }

    @Override
    public Container<R> container() {
        return new Container<>(this);
    }

    @Override
    public void accumulate(Container<R> container, RowView rowView) {
        Path root = container.rootPath(rowView);
        Object key = key(rowView, root.keyLabel);
        if (key == null) {
            return;
        }
        if (accumulateObject(container, root, key, rowView) == null) {
            throw new MappingException("Root mapper returned null for " + root.keyLabel + " = " + describe(key));
        }
        container.rootKeys.add(key);
    }

    @Override
    public Stream<R> stream(Container<R> container) {
        applyLinks(container, new HashSet<>());
        Map<Object, Mapped> roots = container.stateFor(this).objects;
        return container.rootKeys.stream().map(key -> type.cast(roots.get(key).object));
    }

    /**
     * Links the relations of the joined types before the relations of this type, so that a linker receives
     * a complete object unless the graph is recursive.
     */
    private void applyLinks(Container<?> container, Set<JoinRowReducer<?>> visited) {
        if (!visited.add(this)) {
            return;
        }
        NodeState state = container.states.get(this);
        if (state == null) {
            return;
        }
        for (Link<R, ?> link : links) {
            link.target(container).applyLinks(container, visited);
        }
        for (int i = 0; i < links.size(); i++) {
            links.get(i).apply(container, state, state.links.get(i));
        }
    }

    /**
     * Maps the object for the key if this path ranks better than the path it was mapped from, then feeds
     * the row to every relation of this type under this path.
     *
     * @return the object for the key, or null if the mapper returned null
     */
    private Object accumulateObject(Container<?> container, Path path, Object key, RowView rowView) {
        NodeState state = container.stateFor(this);
        Mapped mapped = state.objects.get(key);
        if (mapped == null || path.rank < mapped.path.rank) {
            mapped = new Mapped(path.map(rowView), path);
            state.objects.put(key, mapped);
        }
        for (int i = 0; i < links.size(); i++) {
            Path child = path.children[i];
            if (child != null) {
                links.get(i).accumulate(container, state.links.get(i), child, key, rowView);
            }
        }
        return mapped.object;
    }

    private static String label(String path, String column) {
        return path.isEmpty() ? column : path + "_" + column;
    }

    /**
     * Reads a key so that equal database values compare equal: integral numbers as {@code Long} whatever
     * their JDBC type, decimals without trailing zeros, and binary keys by content.
     */
    private static Object key(RowView rowView, String label) {
        Object key = rowView.getColumn(label);
        if (key instanceof Integer || key instanceof Short || key instanceof Byte) {
            return ((Number) key).longValue();
        }
        if (key instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros();
        }
        if (key instanceof byte[] bytes) {
            return ByteBuffer.wrap(bytes);
        }
        return key;
    }

    private static String describe(Object key) {
        if (key instanceof ByteBuffer buffer) {
            return HexFormat.of().formatHex(buffer.array());
        }
        return String.valueOf(key);
    }

    /**
     * The state of one reduction. The type is package-private, so it cannot be named outside the package and
     * does not appear in the Javadoc. A caller that drives the reducer by hand holds it in a {@code var}, or
     * holds the reducer as a {@code RowReducer<?, R>}. A subclass configures the reducer through the copy
     * constructor and overrides neither {@code accumulate} nor {@code stream}.
     *
     * @param <R> the root type
     */
    static final class Container<R> {
        private final JoinRowReducer<R> root;
        private final Map<Class<?>, JoinRowReducer<?>> nodesByType;
        private final Map<JoinRowReducer<?>, NodeState> states = new IdentityHashMap<>();
        private final Set<Object> rootKeys = new LinkedHashSet<>();
        private Path rootPath;

        private Container(JoinRowReducer<R> root) {
            this.root = root;
            this.nodesByType = root.declaredNodes();
        }

        private NodeState stateFor(JoinRowReducer<?> node) {
            return states.computeIfAbsent(node, n -> new NodeState(n.links.size()));
        }

        private JoinRowReducer<?> nodeFor(Class<?> type) {
            return nodesByType.computeIfAbsent(type, JoinRowReducer::of);
        }

        /**
         * Lays out the paths of the graph that the result set selects, on the first row. A relation is followed
         * on a path when the path has the key column of the joined type. The traversal is breadth first, so the
         * rank of a path orders shallow paths before deep ones and, at one depth, declared relations in order.
         */
        private Path rootPath(RowView rowView) {
            if (rootPath != null) {
                return rootPath;
            }
            Set<String> columns = new HashSet<>();
            for (String column : rowView.getColumnNames()) {
                columns.add(column.toLowerCase(Locale.ROOT));
            }
            if (!columns.contains(root.keyColumn.toLowerCase(Locale.ROOT))) {
                throw new MappingException("The result set has no root key column " + root.keyColumn);
            }
            Path rootCandidate = new Path(root, "", root.keyColumn, 0, mapperFactoryFor(root));
            Map<Link<?, ?>, String> unmatched = new LinkedHashMap<>();
            Set<Link<?, ?>> matched = new HashSet<>();
            int rank = 0;
            Deque<Path> queue = new ArrayDeque<>();
            queue.add(rootCandidate);
            while (!queue.isEmpty()) {
                Path path = queue.remove();
                for (int i = 0; i < path.node.links.size(); i++) {
                    Link<?, ?> link = path.node.links.get(i);
                    JoinRowReducer<?> target = link.target(this);
                    String prefix = label(path.prefix, link.prefix);
                    String keyLabel = label(prefix, target.keyColumn);
                    if (columns.contains(keyLabel.toLowerCase(Locale.ROOT))) {
                        Path child = new Path(target, prefix, keyLabel, ++rank, mapperFactoryFor(target));
                        path.children[i] = child;
                        matched.add(link);
                        queue.add(child);
                    } else {
                        unmatched.putIfAbsent(link, keyLabel);
                    }
                }
            }
            unmatched.keySet().removeAll(matched);
            if (!unmatched.isEmpty()) {
                Map.Entry<Link<?, ?>, String> missing = unmatched.entrySet().iterator().next();
                throw new MappingException("The result set has no key column " + missing.getValue()
                    + " for relation \"" + missing.getKey().prefix + "\" to " + missing.getKey().joinedType.getName());
            }
            rootPath = rootCandidate;
            return rootPath;
        }

        private BiFunction<Class<?>, String, ? extends RowMapper<?>> mapperFactoryFor(JoinRowReducer<?> node) {
            return node.mapperFactory != null ? node.mapperFactory : root.mapperFactory;
        }
    }

    /**
     * One relation path of a reduction: a type reached under a column prefix.
     */
    private static final class Path {
        private final JoinRowReducer<?> node;
        private final String prefix;
        private final String keyLabel;
        private final int rank;
        private final BiFunction<Class<?>, String, ? extends RowMapper<?>> mapperFactory;
        private final Path[] children;
        private RowMapper<?> mapper;

        private Path(JoinRowReducer<?> node, String prefix, String keyLabel, int rank, BiFunction<Class<?>, String, ? extends RowMapper<?>> mapperFactory) {
            this.node = node;
            this.prefix = prefix;
            this.keyLabel = keyLabel;
            this.rank = rank;
            this.mapperFactory = mapperFactory;
            this.children = new Path[node.links.size()];
        }

        private Object map(RowView rowView) {
            if (mapperFactory == null) {
                try {
                    return rowView.getRow(node.type, prefix);
                } catch (NoSuchMapperException e) {
                    if (!prefix.isEmpty()) {
                        throw e;
                    }
                    throw new NoSuchMapperException("No row mapper registered for " + node.type.getName() + " without a prefix."
                        + " Register a reflective mapper such as BeanMapper.factory(" + node.type.getSimpleName()
                        + ".class), or pass a custom mapper through mappedBy()", e);
                }
            }
            if (mapper == null) {
                mapper = requireNonNull(mapperFactory.apply(node.type, prefix), "mapperFactory returned null for " + node.type.getName());
            }
            return rowView.getRow(mapper);
        }
    }

    private static final class NodeState {
        private final Map<Object, Mapped> objects = new HashMap<>();
        private final List<LinkState> links;

        private NodeState(int linkCount) {
            links = new ArrayList<>(linkCount);
            for (int i = 0; i < linkCount; i++) {
                links.add(new LinkState());
            }
        }
    }

    private record Mapped(Object object, Path path) {}

    private static final class LinkState {
        private final Map<Object, Object> oneByParentKey = new LinkedHashMap<>();
        private final Map<Object, Set<Object>> manyByParentKey = new LinkedHashMap<>();
    }

    private record Link<R, J>(String prefix, Class<J> joinedType, JoinRowReducer<J> joined, BiConsumer<? super R, ? super J> linker, boolean many) {
        Link {
            requireNonNull(prefix, "prefix");
            requireNonNull(joinedType, "joinedType");
            requireNonNull(linker, "linker");
            if (prefix.isEmpty()) {
                throw new IllegalArgumentException("A relation needs a column prefix");
            }
        }

        private JoinRowReducer<?> target(Container<?> container) {
            return joined != null ? joined : container.nodeFor(joinedType);
        }

        void accumulate(Container<?> container, LinkState state, Path path, Object parentKey, RowView rowView) {
            Object joinedKey = key(rowView, path.keyLabel);
            if (joinedKey == null) {
                return;
            }
            path.node.accumulateObject(container, path, joinedKey, rowView);
            if (many) {
                state.manyByParentKey.computeIfAbsent(parentKey, k -> new LinkedHashSet<>()).add(joinedKey);
                return;
            }
            Object previous = state.oneByParentKey.putIfAbsent(parentKey, joinedKey);
            if (previous != null && !previous.equals(joinedKey)) {
                throw new MappingException("Column " + path.keyLabel + " has more than one value (" + describe(previous) + ", "
                    + describe(joinedKey) + ") for the same parent, so the relation is not to-one. Declare it with many() instead of one()");
            }
        }

        void apply(Container<?> container, NodeState parentState, LinkState state) {
            NodeState targetState = container.states.get(target(container));
            if (targetState == null) {
                return;
            }
            state.oneByParentKey.forEach((parentKey, joinedKey) -> link(parentState, targetState, parentKey, joinedKey));
            state.manyByParentKey.forEach((parentKey, joinedKeys) -> joinedKeys.forEach(joinedKey -> link(parentState, targetState, parentKey, joinedKey)));
        }

        @SuppressWarnings("unchecked")
        private void link(NodeState parentState, NodeState targetState, Object parentKey, Object joinedKey) {
            Object parent = parentState.objects.get(parentKey).object;
            Object joined = targetState.objects.get(joinedKey).object;
            if (parent != null && joined != null) {
                linker.accept((R) parent, (J) joined);
            }
        }
    }
}
