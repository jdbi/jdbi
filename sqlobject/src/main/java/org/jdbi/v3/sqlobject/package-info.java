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
/**
 * <h2>SQL Objects</h2>
 * <p>
 * The SQLObject API allows for declarative definition of interfaces which will handle
 * the generation of statements and queries on your behalf when needed. Take the following interface:
 * </p>
 * <pre>
 * public interface TheBasics {
 * &#64;SqlUpdate("insert into something (id, name) values (:id, :name)")
 * int insert(&#64;BindBean Something something);
 * &#64;SqlQuery("select id, name from something where id = :id")
 * Something findById(&#64;Bind("id") long id);
 * }
 * </pre>
 * <p>
 * First, install the SQL Object plugin:
 * </p>
 * <pre>
 * Jdbi jdbi = Jdbi.create(dataSource);
 * jdbi.installPlugin(new SqlObjectPlugin());
 * </pre>
 * <p>
 * You can obtain an instance of <code>TheBasics</code> via one of three means.
 * </p>
 * <ul>
 * <li>
 * <p>
 * You can pass a lambda to Jdbi. A short-lived instance of the interface will be created, and passed to the
 * lambda. The lambda can make any number of calls to that instance before returning. The lifecycle of
 * the SQL Object instance ends when the lambda returns.
 * </p>
 * <pre>
 * jdbi.useExtension(TheBasics.class, theBasics -&gt; theBasics.insert(new Something(1, "Alice")));
 * Something result = jdbi.withExtension(TheBasics.class, theBasics -&gt; theBasics.findById(1));
 * </pre>
 * <p>
 * <code>withExtension</code> returns the value returned by the lambda, whereas <code>useExtension</code>
 * has a void return.
 * </p>
 * </li>
 * <li>
 * <p>
 * You can get an instance attached to a particular handle. The SQL Object's lifecycle ends when the
 * handle is closed.
 * </p>
 * <pre>
 * try (Handle handle = jdbi.open()) {
 * TheBasics attached = handle.attach(TheBasics.class);
 * attached.insert(new Something(2, "Bob");
 * Something result = attached.findById(2);
 * }
 * </pre>
 * </li>
 * <li>
 * <p>
 * Finally, you can request an on-demand instance. On-demand instances have an open-ended lifecycle, as they
 * obtain and releases connections for each method call. They are thread-safe, and may be reused across an
 * application. This is handy when you only need to make single calls at a time.
 * </p>
 * <pre>
 * TheBasics onDemand = jdbi.onDemand(TheBasics.class);
 * onDemand.insert(new Something(3, "Chris"));
 * Something result = onDemand.findById(3);
 * </pre>
 * <p>
 * There is a performance penalty every time a connection is allocated and released. If you need to make
 * multiple calls in a row to a SQL Object, consider using one of the above options for
 * better performance, instead of on-demand.
 * </p>
 * </li>
 * </ul>
 * <h2>Default Methods</h2>
 * <p>
 * You can declare default methods on your interface, which can call other methods of the interface:
 * </p>
 * <pre>
 * public interface DefaultMethods {
 * &#64;SqlQuery("select * from folders where id = :id")
 * Folder getFolderById(int id);
 * &#64;SqlQuery("select * from documents where folder_id = :folderId")
 * List&lt;Document&gt; getDocuments(int folderId);
 * default Node getFolderByIdWithDocuments(int id) {
 * Node result = getById(id);
 * result.setChildren(listByParendId(id));
 * return result;
 * }
 * }
 * </pre>
 * <h2>Mixin Interfaces</h2>
 * <p>
 * All SQL objects implicitly implement the <code>SqlObject</code> interface (whether you declare it or not), which
 * provides a <code>getHandle()</code> method. This is handy when you need to "drop down" to the core API for
 * scenarios not directly supported by SQL Object:
 * </p>
 * <pre>
 * public interface UsingMixins extends SqlObject {
 * &#64;RegisterBeanMapper(value={Folder.class, Document.class}, prefix={"f", "d"})
 * default Folder getFolder(int id) {
 * return getHandle().createQuery(
 * "select f.id f_id, f.name f_name, " +
 * "d.id d_id, d.name d_name, d.contents d_contents " +
 * "from folders f left join documents d " +
 * "on f.id = d.folder_id " +
 * "where f.id = :folderId")
 * .bind("folderId", id)
 * .reduceRows(Optional.&lt;Folder&gt;empty(), (folder, row) -&gt; {
 * Folder f = folder.orElseGet(() -&gt; row.getRow(Folder.class));
 * if (row.getColumn("d_id", Integer.class) != null) {
 * f.getDocuments().add(row.getRow(Document.class));
 * }
 * return Optional.of(f);
 * });
 * }
 * }
 * </pre>
 * <p>
 * Any interface that extends <code>SqlObject</code> can be used as a SQL Object mixin, provided all of its methods
 * have a SQL method annotation (e.g. <code>&#64;SqlQuery</code>, or are interface default methods.
 * </p>
 * <h2>Transactions</h2>
 * <p>
 * Any SQL Object method annotation with <code>&#64;Transaction</code> will be executed within a transaction.
 * This is most commonly used on interface default methods to roll up multiple method calls:
 * </p>
 * <pre>
 * public interface TransactionAnnotation {
 * &#64;SqlUpdate("insert into folders (id, name) values (:id, :name)")
 * void insertFolder(&#64;BindBean Folder folder)
 * &#64;SqlBatch("insert into documents (id, folder_id, name, content) " +
 * "values (:id, :folderId, :name, :content)")
 * void insertDocuments(int folderId, &#64;BindBean List&lt;Document&gt; documents);
 * &#64;Transaction
 * default void insertFolderWithDocuments(Folder folder) {
 * insertFolder(folder);
 * insertDocuments(folder.getId(), folder.getDocuments());
 * }
 * }
 * </pre>
 * <p>
 * Jdbi also provides a <code>Transactional</code> mixin interface. When a SQL Object type extends this interface,
 * callers may invoke method from that interface to manage transactions:
 * </p>
 * <pre>
 * public interface TransactionalWithDefaultMethods extends Transactional {
 * &#64;SqlUpdate("insert into folders (id, name) values (:id, :name)")
 * void insertFolder(&#64;BindBean Folder folder)
 * &#64;SqlBatch("insert into documents (id, folder_id, name, content) " +
 * "values (:id, :folderId, :name, :content)")
 * void insertDocuments(int folderId, &#64;BindBean List&lt;Document&gt; documents);
 * }
 * </pre>
 * <p>
 * Thus:
 * </p>
 * <pre>
 * TransactionalWithDefaultMethods dao = jdbi.onDemand(TransactionalWithDefaultMethods.class);
 * dao.inTransaction(self -&gt; {
 * self.insert(folder);
 * self.insertDocuments(folder.getId(), folder.getDocuments());
 * });
 * </pre>
 * <p>
 * <b>
 * Note: use caution combining <code>Transactional</code> with on-demand SQL Objects.
 * </b>
 * The only methods considered safe to call with on-demand SQL Objects are <code>inTransaction</code> or
 * <code>useTransaction</code>.
 * </p>
 */
package org.jdbi.v3.sqlobject;
