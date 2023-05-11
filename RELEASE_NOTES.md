# Unreleased

  - allow unknown result mappers during `ResultReturner` warmup. This restores the pre-3.38.0 behavior
    where SQLObject classes with invalid methods could be used unless a method is explicitly called (#2342)
  - document vavr incompatibility between 0.10.x and 1.0.0-alpha (#2350)
  - Handle.inTransaction: improve exception thrown when restoring transaction isolation #2343
  - add support for Guice 6.x (using javax.inject annotations) and guice 7.x (using jakarta.inject annotations)

# 3.38.2
  - spring5 JdbiUtil: fix thread safety #2341

# 3.38.1
  - Dramatic performance improvement around copying configuration objects when creating statements
    Should reduce allocation rate and time spent in ConcurrentHashMap init

# 3.38.0

  - `CaseStrategy` is now an official API (dropped @Beta) (#2309)
  - rewrite `AccessibleObjectStrategy` into an enum (#2310)
  - allow user defined functions for message rendering (#2311)
  - fix `GenericType` creation in parameterized classes (#2305)
  - add `SqlStatements#setAttachAllStatementsForCleanup`. Setting this configuration flag will attach all created statements to their handles for resource cleanup. Default is `false`. (#2293, thanks @jodastephen)
  - add `SqlStatements#setAttachCallbackStatementsForCleanup`. Setting this configuration flag will attach all created statements within one of the `Jdbi` callback methods to the handle. This allows code that uses the `Jdbi` callback methods to delegate resource management fully to Jdbi. This flag is set by default. (#2293, thanks @jodastephen)
  - fix problem when using the jdbi bom in spring projects (#2295, thanks @jedvardsson)
  - add `JdbiExecutor` for async Jdbi operations (#2280, thanks @leblonk)
  - rewrite the core extension framework, move functionality from sqlobject to core
  - rewrite sqlobject and the generator to use the new extension framework, deprecate functionality that moved to the core
  - StringTemplate engine: handle st4 errors rather than logging to stderr. Allow configuring missing attribute as a fatal error
  - StringTemplate 4.3.4
  - update spring framework dependency to 5.3.27 (address CVE-2023-20861, CVE-2023-20863)
  - ResultIterable methods: `set()`, `collectToMap()`, `toCollection()`, `collectInto()`, `collectIntoList()`, `collectIntoSet()`
  - Helpers to make registering CollectorFactory easier
  - FreemarkerEngine encourages singleton use so caching works better
  - Support Consumer<Iterable<T>> as a consumer argument for operations that return multiple results.

# 3.37.1
  - fix deadlock in default Jdbi cache (#2274)

# 3.37.0

** DO NOT USE **

The default cache contains a thread deadlock when the cache is
evicting while adding new entries through multiple threads.  This has
been fixed in 3.37.1

  - upgrade to geantyref 1.3.14
  - removes the core dependency on the caffeine library. This now uses a simple LRU cache for sql parser and sql statements.
  - adds pluggable cache implementation using caffeine. The old caching behavior can now be restored by using the
    `jdbi3-caffeine-cache` dependency and adding `jdbi.installPlugin(new CaffeineCachePlugin());`.
  - adds pluggable no-op cache implementation for testing and debugging
  - improve PostGIS null value handling (#2260, thanks @tprebs)
  - use Postgres 15 (native arm64) for building on MacOS arm64

# 3.36.0

  - fix concurrency issue when copying the config registry (#2236), thanks @npetryk
  - Support class-level (static) instance fields for JdbiExtension and its subclasses.
  - Add jdbi3-testing support for testcontainer based databases, see http://jdbi.org/#_using_testcontainers for details

# 3.35.0

  - Fix `JdbiFlywayMigration` to work with Flyway 9 (#2179, thanks @broccolai)
  - ResultIterable.useIterator and ResultIterable.withIterator new helper methods to close iterator resources
  - add handle and resource leak checking to all unit tests, fix unit tests to not leak any resources
  - add resource leak checking support to the `JdbiExtension` JUnit5 testing framework
  - support lifecycle listeners for `Handle` and `StatementContext`
  - fixes and updates to the build system, additional docs for contributing, IDE code style support for IntelliJ and Eclipse
  - doc updates for Kotlin code
  - add Kotlin `mapTo(KClass<*>)` extension function to `ResultBearing`
  - SqlObject Consumer<T> now accepts Consumer<Stream<T>> and Consumer<Iterator<T>> forms
  - deprecate misnamed `setTransactionIsolation` method, add `setTransactionIsolationLevel` to the handle
  - deprecate misnamed `release` method, add `releaseSavepoint` to the handle
  - add missing `isInTransaction` method to `Transactional`, bringing it to par to the handle set of methods
  - add H2 option string, user and password methods to `JdbiH2Extension`
  - ReflectionMappers: add accessibleObjectStrategy to allow skipping setAccessible calls for FieldMapper in particular
  - minimal support for testing with other databases in `JdbiGenericExtension`
  - Dependabot warnings for Postgres and H2, upgrade to 42.5.1 and 2.1.214 respectively.

# 3.34.0

  - `jdbi3-generator` will now support any Java 8+ version without generating compile-time warnings (#2128)
  - AbstractArgumentFactory also need to check for supertypes when the generic argument is not a class (fixes #2026)
  - Replace `@Unmappable` with `@JdbiProperty` so you can configure both mapping and binding flexibly

# 3.33.0

  - make `@Unmappable` work with FieldMapper fields and KotlinMapper properties
  - rework the mapping logic for all bean related mappers (Bean, Field, Constructor, KotlinMapper)
  - clarify the @PropagateNull logic, ensure that it works with nested beans
  - ensure that bean mapper annotations work with lombok value and bean classes
  - add explicit support for `byte[]` -> `BYTEA` for Postgres. (#2109)
  - Revert lexer changes #1906 due to regressions
  - add missing jdbi3-postgis and jdbi3-json (tests) to the BOM
  - update build tooling for Java and Kotlin
  - internal kotlin packages are considered undocumented and no longer show up in the docs
  - Bean mapping now prefers setter annotations over getter annotations. (#2103)
  - Various methods that accept SQL statements now take CharSequence in preference to String (#2047)
  - Add a typesafe `Sql` class for SQL statements.
  - Upgrade Postgres driver to 42.5.0
  - call warm() correctly for SqlStatementCustomizer (#2040)

# 3.32.0

  - Use Kotlin 1.7 ecosystem (compiler, plugin) but compile to Kotlin 1.5 compatibility
  - Add support for Eclipse LocationTech JTS data types with PostGIS. (#2072, #2074, thank you @bchapuis)
  - Fix exception in Handle#close() when underlying connection is closed (#2065)
  - Give access to per-batch row modification count (#2069, #2060, thank you @doppelrittberger)
  - Start new `examples` module for JDBI3 example code.

# 3.31.0

  - Support binding parameters of type CharSequence (#2057, thanks @sman-81)
  - Fix Sql scripts to support BEGIN / END nested scripts (#2021, thanks @sman-81)
  - ResultIterables have additional convenience methods forEachWithCount and filter (#2056, thanks @sman-81)
  - upgrades to the build system to support external modules. This allows keeping the `jdbi3-oracle12` module up to date.

# 3.30.0

  - Fix DefinedAttributeTemplateEngine lexer bug swallowing single-quoted text with escapes (#1906)
  - ANTLr 4.10.1
  - GSON 2.9.0 fixes CVE-2022-25647
  - Spring 5.3.20 fixes CVE-2022-22965
  - Promote TemplateEngine.NOP to a NoTemplateEngine class, for use with @UseTemplateEngine

# 3.29.0

  This is a maintenance release. It bumps the minor because of a backwards incompatible change
  in the `JdbiOtjPostgresExtension` due to a change in the upstream `otj-pg-embedded` component.

  If you do not use this component, there are no significant changes over 3.28.0.

  - build now fully supports building with JDK 17
  - minor changes and cleanups (#2020, #2023)
  - always load kotlin plugin if using kotlin-sqlobject (#2023)
  - change BOM to resolve versions in the released bom version
  - update to otj-pg-embedded 1.0.1 (0.13.x started to crash on MacOS Monterey). This is a backwards
    incompatible change as the component changed the call signature of `getJdbcUrl`. This only
    affects the JdbiOtjPostgresExtension

# 3.28.0
  - Remove the antlr4-runtime dependency by inlining it into the core jar.
  - [CI] add lgtm checks
  - [CI] build integration tests for inlined jar
  - [SECURITY] update baseline dependencies for known CVE (reported by dependabot)

# 3.27.2
  - Fix NPE in SqlLogger#logAfterExecution when query string is not available (#2000), thanks @tmichel!

# 3.27.1
  - Fix Batch binding with serializable transaction retries (#1967), thanks @sudharsannr!

# 3.27.0

  - Fix serious regression in 3.26.1: incorrect Batch parameter binding

# 3.26.1

** DO NOT USE **

Serious regression in batch binding: https://github.com/jdbi/jdbi/issues/1987

  - Fix transaction callbacks working with nested transactions

# 3.26.0
  - change annotation targets for Kotlin annotations to `CLASS` (fixes #1971)
  - use h2 2.x for unit tests (prevents spurious "security" reports)
  - run more integration tests for postgres and dependency compatibility

# 3.25.0
  - *SPI change* `@Json String` database type mappers now use `@EncodedJson String` instead (#1953)
  - Sql4JSqlLogger: fix NPE when using Script
  - fix using ConstructorMapper on empty generated keys results (#1955)
  - Handle: add new afterCommit and afterRollback transaction callbacks

# 3.24.1
  - fix Bean property arguments type being erased on generic beans

# 3.24.0
  - Fix batch binding with mixed null and non-null primitives (#1901)
  - Add Arguments.setPreparedArgumentsEnabled to disable new preparable arguments feature
  - Add JUnit 5 support to the testing package.
  - Add interceptors for type inference for Row Mappers, Column Mappers and SQL Array types
  - Fix mapper lookup for Kotlin data types. Ensure that registered prefixes are used. Fixes #1944
  - `RowMappers#register(RowMapper<?>)` no longer allows `Object` as concrete parameter type.
  - Run ktlint and detekt on Kotlin code to improve code quality

# 3.23.0
  - Rework and document importing bindings in guice definition modules
  - expose createKey() method in guice definition modules
  - Add no-op GuiceJdbiCustomizer
  - Fix annotation inheritance for non-direct supertypes (#1920)
# 3.22.0
  - Guice support for Jdbi creation and injection (#1888)
  - documentation fixes
  - update CI to build with post-11 JDK (currently 16)
# 3.21.0
  - Fix sending Long types as INT8 instead of INT4 (#1902)
  - Updates to build and compilation (e.g. treat parameters/noparameters correctly)
  - Add a local `mvnw` binary to build without a local maven installation
  - Documentation updates and deploy fixes (kotlin docs work again)
  - Add a Metadata API to allow Database metadata access through the Handle (#1889)

# 3.20.1
  - Allow installation of the PostgresPlugin without unqualified HStore bindings (#1875)
  - Update Kotlin to 1.5.0
  - Update Caffeine dependency to 3.0.2
  - Add missing jdbi3-spring5 to the JDBI bom
  - minor code fixes

# 3.20.0
  - Promote Postgres LOB APIs to stable.
  - Promote JSON, Jackson, and Gson APIs to stable.
  - Actually deploy Spring5 artifacts...
  - New RowMapper and ColumnMapper init hook

# 3.19.0
**Java8 compatibility note!**
  - Jdbi now requires Java 11+ to build (still runs on 8, for now...)
    - upgrade Caffeine dep to 3.0.1 for jdk16 (NOTE: Java8 users will need to manage it back to 2.x)

**Spring 4 support deprecated due to upstream EOL (this will be the last release!) Please move to Spring 5**

  - Simple slf4j SqlLogger implementation to debug all statements executed
  - new RollbackOnlyTransactionHandler rolls back everything for testing
  - add support for Moshi JSON mapping (#1809, thanks unoexperto!)
  - Register more array types like `boolean` out of the box, #1802
  - add Codec (combination of ArgumentFactory and ColumnMapper) to provide one API for serialization/deserialization.
  - add new @Alpha annotation for even less stable new features
  - Promote a number of previous Beta APIs to stable.

# 3.18.1
  - Comments like -- and // now recognized and discarded from SQL, thanks @rherrmann!
  - ANTLR 4.9

# 3.18.0
  - Result collecting now accepts ? super T wildcard
  - Field and method Argument binding now uses the generic type, instead of the erased type

# 3.17.0
  - support @BindPojo with properties that vary in generically-typed pojos
  - factor out TemplateEngine cache into SqlStatements to avoid unbounded memory usage with dynamic SQL
  - new beta interface TemplateEngine.Parsing for custom template engines that want to use built in caching

# 3.16.0
  - SqlObject decorators (and particularly @Transaction) now supported on types

# 3.15.1
  - fix LinkageError from ConfigRegistry in multi-classloader environments

# 3.15.0
  - Significant performance improvements for data intensive workloads
    - new @Beta warm() method on Handler, SqlStatement*Customizer to pre-initialize Config instances
  - bind() overloads for UUID and URI, see #1765

# 3.14.5
  - SqlBatch: fix inserting of constant `null` value, #1761

# 3.14.4
  - fix performance regression on statement preparation, #1732

# 3.14.3
  - fix ThreadLocal leak warning for Tomcat
  - AnnotationFactory: try to use class's ClassLoader before going to TCCL

# 3.14.2
  - FreeBuilder documentation
  - jpa: NPE when deserializing read-only properties

# 3.14.1
  - FreeBuilders: fix lookup of builder classes

# 3.14.0
  - immutables: support getter with @ColumnName, #1704
  - postgres: simple CRUD support for LargeObject API
  - kotlin-sqlobject: fix package declaration of RegisterKotlinMappers
  - LocalTransactionHandler: bind more closely to Handle for performance and to avoid leaks
  - Update: add `one` helper method
  - Array: fix repeated binding of Iterable types, #1686
  - Initial support for FreeBuilders based pojo types

# 3.13.0
  - Kotlin: respect default values in methods when passed null, #1690
  - `Arguments.bindingNullToPrimitivesPermitted` helps you catch
  erroneous binding of `null` to a primitive type
  - preparable Arguments: re-invoke prepare on each copy, to propagate updated configuration

# 3.12.2
  - Bean binding: ignore getter methods with parameters
  - Immutables: find builder set method even with @ColumnName
  - CachingSqlParser: default limit to 1000 parsed statements, #1658
  - bom: don't inherit from parent #1656
  - bean mapping: improve detection of incomplete wildcard types

# 3.12.0
  - `EnumSet` can be bound and mapped as a bitstring to a Postgres `varbit` column
    (requires appropriate use of `@SingleValue`).
  - fix detecting Enum anonymous subclasses (types with overridden methods)

# 3.11.1
  - fix PreparedBatch binding non-prepareable nested arguments

# 3.11.0
  - Argument: allow direct binding of Argument subtypes
  - Immutables: use `@ColumnName("name")` to override property-derived names

Major Performance Rework: ArgumentFactory is now Prepareable
Prepared arguments must select an ArgumentFactory implementation
based only on the qualified type, not the value.  This dramatically
increases performance for large batch inserts.

# 3.10.1
  - SqlArray: Fix binding Postgres double[] / float[]

# 3.10.0
- New Features
  - Handle.getJdbi gets owning Jdbi instance
  - SqlStatement (like Query) has new bindArray helper methods
  - sqlobject's `EmptyHandling` enum backported to core for invocations of `SqlStatement.bindList`
  - OutParameters lets you `getRowSet` to view cursor-typed out parameters
  - Call.invoke lets you process OutParameters before closing the statement with a Consumer or Function
  - @SqlCall lets you process OutParameters before closing the statement by passing a Consumer or Function
  - installPlugin skips duplicate JdbiPlugins (according to Object.equals)
  - KotlinSqlObjectPlugin will install forgotten SqlObjectPlugin for you
  - ClasspathSqlLocator allows disabling comment stripping and deprecate static API
  - KotlinMapper respects `@PropagateNull`
  - Freemarker allows customizing Configuration
  - FreemarkerSqlLocator should now be created via FreemarkerConfig so it shares config
- New Beta Features
  - added `register` methods for qualified factories on `Configurable`,
    `ColumnMappers`, and `ArgumentFactories`
- Bug Fixes
  - onDemand invocations @CreateSqlObject create new on-demand SqlObjects
  - onDemand SqlObject.withHandle / Transactional.inTransaction are now safe to call even outside an on-demand context
  - SqlParsers no longer retain all statements and instead use a `caffeine` cache
- Compatibility
  - added a module that runs the Spring 4 integration tests against Spring 5 to monitor forward compatibility
  - OutParameters no longer has a public constructor (this type should never really have been constructed anyway)

# 3.9.1
- Bug Fixes
  - fix minor PreparedBatch statement leak
  - fix statement summarization NPE
- Improvements
  - minor Optional mapping performance improvement

# 3.9.0
- New Features
  - `ResultIterable<T>.map(Function<T, U>)` returns a `ResultIterable<U>` with elements transformed
    using the given mapper function.
  - `ColumnMappers.setNullPrimitivesToDefaults(boolean)` allows you to decide if database nulls
    should become Java primitive defaults or a mapping exception.
  - `@PropagateNull` annotation allows a missing property to indicate a missing compound value
  - `@DatabaseValue` annotation lets you customize enum values as stored in the DB
- Bug Fixes
  - Immutables integration doesn't respect @Value.Default for primitives that are nulled in the db
- Improvements
  - Immutables: handle `is` prefix more intelligently
  - `StatementExceptions` configuration class lets you configure generated exception message length
- Changes
  - Align PreparedBatch with no bindings behavior to normal empty Batch (return no results)
- Beta API Changes
  - add GenericTypes.box
  - QualifiedType: rename mapType -> flatMapType, add a proper mapType

# 3.8.2
- Improvements
  - `jdbi3-gson2` artifact is now managed in `jdbi3-bom`.
  - SQL script parser no longer treats Postgres JSON operators `#>` or `#>>` as comments.
  - Improved caching reduces garbage generation from Jdbi's SQL parsing internals.

# 3.8.1
- Bugfixes
  - provide SqlBatch statement customizers with non-`null` Statement

# 3.8.0
- New Features
  - `RowViewMapper` lets you use higher level `RowView` in a `RowMapper`.
  - `ResultIterable.first()` returns the first row. Throws an exception if there are zero rows.
  - `ResultIterable.one()` returns the only row. Throws an exception if there are zero or multiple
    rows.
  - `ResultIterable.findOne()` returns an `Optional<T>` of the only row, or `Optional.empty()` if
    there are zero rows, or the only row is `null`. Throws an exception if there are multiple rows.
- Deprecated API
  - `ResultIterable.findOnly()` is deprecated, in favor of the new method `ResultIterable.one()`.

# 3.7.1
- New Features
  - Initial Java Module System support via Automatic-Module-Name
- Improvements
  - Postgres JDBC driver is now <scope>provided</scope> to avoid fighting with servlet containers.

# 3.7.0
- New Features
  - @AllowUnusedBindings SqlObject customizer, like SqlStatements.setUnusedBindingsAllowed
  - Enums config class to change the default policy for binding and mapping Enum values.
  - @UseEnumStrategy SqlObject configurer for setting the default enum strategy.
  - @EnumByName and @EnumByOrdinal qualifying annotations to override said default policy.
  - Support for Postgres custom types, both user defined and driver types like `PGInterval` or `PGcircle`
  - RowView: add getColumn(QualifiedType) overloads
  - SetObjectArgumentFactory and GetObjectColumnMapperFactory to easily make use of direct type support
    provided by your database driver (e.g. many databases now directly support `java.time` objects).
  - simple Jackson2 `@JsonView` support
  - @Unmappable lets you skip properties during pojo / bean mapping
- Beta Api Changes
  - Qualifiers.qualifiers renamed to findFor, restyled as JdbiConfig configuration class
- Bugfixes
  - Improve vavr handling of typed null
- Improvements
  - Improve Error handling with transactions
  - Clean up our dependencies, remove vestiges of `shade` plugin
  - Upgrade to antlr4
  - Rework caching to not use extra threads (#1453)
  - Any valid Java identifier is now supported as a named parameter (e.g. `:제목`) or defined attribute
    (e.g. `<제목>`).
  - Nested `inTransaction` and `useTransaction` calls are now allowed in core, provided the inner
    transaction specifies the same transaction isolation level, or does not specify isolation. This
    brings core transaction behavior in line with the existing behavior for nested `@Transaction` SQL
    object methods.
  - Nested calls on a thread to `Jdbi` methods `useHandle`, `withHandle`, `useTransaction`,
    `withTransaction`, `useExtension`, `withExtension`, or to any method of an on-demand extension will
    now execute against the handle from the outermost call, rather than each invocation getting a separate
    handle.
- Minor source incompatibility
  - JdbiPlugin methods now `throws SQLException`

# 3.6.0
- New Features
  - ConnectionFactory now also may customize connection closing
  - GenericTypes.findGenericParameter(Type, Class) now also takes an index, e.g. to resolve `V`
    in `Map<K, V>`
  - @JdbiConstructor can now be placed on a static factory method
  - GenericMapMapperFactory enables fluent API and SqlObject support for mapping homogenously
    typed rows (e.g. "select 1.0 as low, 2.0 as medium, 3.0 as high") to `Map<String, V>` for any
    `V` that can be handled by a ColumnMapper.
  - ResultBearing.mapToMap overloads to use the GenericMapMapperFactory
  - ParsedSql can be created with ParsedSql.of(String, ParsedParameters) factory
  - ParsedParameters can be created with ParsedSql.positional(int) and
    ParsedSql.named(List<String>) factories.
  - SQL array registration improvements:
    - SqlArrayType.of(String, Function) / SqlArrayTypeFactory.of(Class, String, Function) factory methods
    - Configurable.registerArrayType(Class, String, Function) convenience method
  - Sqlite support in JdbiRule (jdbi3-testing)
  - TimestampedConfig now controls the ZoneId used to generate an OffsetDateTime
  - StatementCustomizer now has a hook for before SQL statement templating
- New beta API
  - Type qualifiers for binding and mapping. Use annotations to distinguish between different SQL
    data types that map to the same Java type. e.g. VARCHAR, NVARCHAR, and Postgres MACADDR all
    map to String, but are bound and mapped with different JDBC APIs.
  - Support for NVARCHAR columns, using the @NVarchar qualifying annotation
  - Support for Postgres MACADDR columns, using the @MacAddr qualifying annotation
  - Support for HSTORE columns, using the @HStore annotation
  - @Json type qualifier with Jackson 2 and Gson 2 bindings
  - Initial support for Immutables value objects
  - SqlStatement.defineNamedBindings and @DefineNamedBindings let you copy bindings to definitions
- Oracle DB support changes
  - Due to ongoing stability problems with Oracle's Maven servers, we have split the
    jdbi3-oracle12 artifact out of the main project, to a new home at
    https://github.com/jdbi/jdbi3-oracle12. This means that jdbi3-oracle12 versions will no
    longer stay in sync with the rest of Jdbi. Accordingly, we have removed jdbi3-oracle12
    from the BOM.
- API changes
  - SQLitePlugin now has the ServiceLoader manifest it deserves for automagical installation.

# 3.5.2
- Bug Fixes
  - bindList throws an NPE if called with an immutable list,
    method is safe according to the specification
- Improvements
  - improve binding private implementations of interfaces
  - improved loggability (through SqlLogger) of JDBI's built-in Argument instances

# 3.5.1
(whoops, 3.5.0 was released from the wrong commit!)
- New API
  - SqlStatements.allowUnusedBindings allows you to bind Arguments to query parts that may be
    left out of the final query (e.g. by a TemplateEngine that renders conditional blocks)
    without getting an Exception.
  - Added the MapMappers JdbiConfig class to configure column name case changes, preferred over
    the old boolean toggle.
  - ColumnNameMatcher.columnNameStartsWith() method, used by reflection mappers to short-circuit
    nested mappings when no columns start with the nested prefix.
  - bindMethodsList and @BindMethodsList create VALUES(...) tuples by calling named methods
- Improvements
  - SqlObject no longer transforms non-Runtime exceptions (slightly breaking change)
  - Use MethodHandles over Reflection to additionally do less exception wrapping / transformation
  - Skip unused string formatting for performance
  - Spring FactoryBean better singleton support
  - KotlinMapper respects constructor annotations, lateinit improvements
  - Behavioral fixes in Argument binding where the number of provided Arguments differs from the
    expected number.
  - Optional mapping of @Nested objects when using BeanMapper, ConstructorMapper, FieldMapper, or
    KotlinMapper. @Nested objects are only mapped when the result set contains columns that match
    the nested object.
  - ConstructorMapper allows constructor parameters annotated @Nullable to be missing from the
    result set. Any annotation named "Nullable" from any package may be used.
  - jdbi3-testing artifact has pg dependencies marked as optional, in case you e.g. only want h2
    or oracle
  - LocalTransactionHandler: rollback on thrown Throwable
  - test on openjdk11
  - EnumSet mapping support

# 3.4.0
NOTE: this release's git tags are missing due to maintainer error!
- New API
  - StatementException.getShortMessage
  - SqlStatements.setQueryTimeout(int) to configure the JDBC Statement queryTimeout.
- Bug Fixes
  - Bridge methods cause SqlObject exceptions to get wrapped in `InvocationTargetException`
  - Ignore static methods on SqlObject types
- Improvements
  - Handle `null` values in defined attributes

# 3.3.0
- New API
  - SerializableTransactionRunner.setOnFailure(), setOnSuccess() methods allow callbacks to be
    registered to observe transaction success and failure.
  - JdbiRule.migrateWithFlyway() chaining method to run Flyway migrations on the test database
    prior to running tests.
  - @UseStringSubstitutorTemplateEngine SQL object annotation.
  - @Beta annotation to identify non-final APIs.
    - Application developers are invited to try out beta APIs and provide feedback to help us
      identify weaknesses and make improvements before new APIs are made final.
    - Library maintainers are discouraged from using beta APIs, as this might lead to
      ClassNotFoundExceptions or NoSuchMethodExceptions at runtime whenever beta APIs change.
- Improvements
  - Added some extra javadoc to SqlLogger
  - @UseTemplateEngine now works with MessageFormatTemplateEngine and
    StringSubstitutorTemplateEngine
- Bug fixes
  - SqlStatement.bindMethods() (and @BindMethods) now selects the correct method when the method
    return type is generic.
  - mapToMap() no longer throws an exception on empty resultsets when
    ResultProducers.allowNoResults is true
- Breaking changes
  - Remove JdbiRule.createJdbi() in favor of createDataSource(). This was necessary to facilitate
    the migrateWithFlyway() feature above, and pave the way for future additions.
  - Remove SqlLogger.wrap() added in 3.2.0 from public API.
  - Convert MessageFormatTemplateEngine from an enum to a plain class with a public constructor.
    The INSTANCE enum constant has been likewise removed.
  - Remove StringSubstitutorTemplateEngine.defaults(), .withCustomizer() factory methods, in
    favor of the corresponding public constructors.

# 3.2.1
- Fix IllegalArgumentException "URI is not hierarchical" in FreemarkerSqlLocator.

# 3.2.0
- New modules:
  - jdbi3-testing - JdbiRule test rule for JUnit tests
  - jdbi3-freemarker - render SQL templates using FreeMarker
  - jdbi3-commons-text - render SQL templates using Apache commons-text StringSubstitutor
  - jdbi3-sqlite - plugin for use with SQLite database
- New API
  - @SqlScript annotation to execute multiple statements
  - SqlLogger for logging queries, timing, and exceptions. Replacing TimingCollector, which
    is now deprecated
  - Add ResultProducers.allowNoResults configuration option in case you may or may not get a
    result set
  - MessageFormatTemplateEngine template engine, renders SQL using java.text.MessageFormat
  - SqliteDatabaseRule test rule (in jdbi3-core test jar)
- Improvements
  - @MaxRows.value() may now be omitted when used as a parameter annotation
  - SerializableTransactionRunner 'max retries' handling throws more meaningful exceptions
  - Postgres operators like '?' and '?|' may now be used without being mistaken for a positional
    parameter. Escape them in your SQL statements as '??' and '??|', respectively.
  - Support for binding OptionalInt, OptionalLong, and OptionalDouble parameters.
- Bug fixes:
  - SqlObject default methods now work in JDK 9
  - SqlObject no longer gets confused about result types due to bridge methods
  - StringTemplate no longer shares template groups across threads, to work around concurrency
    issues in StringTemplate project
  - DefineStatementLexer handles predicates that look like definitions better. No more errors
    on unmatched "<" when you really meant "less than!"
  - LocalDate binding should store the correct date when the server and database are running
    in different time zones.

# 3.1.1
- Improve IBM JDK compatibility with default methods
- Allow non-public SqlObject types!!!
- Fix some ThreadLocal and StringTemplate leaks

# 3.1.0
- The strict transaction handling check in Handle.close() may be disabled via
  getConfig(Handles.class).setForceEndTransactions(false).
- StringTemplate SQL locator supports StringTemplate groups importing from other groups.
- New RowReducer interface and related APIs make it simple to reduce master-detail joins
  into a series of master objects with the detail objects attached. See:
  - RowReducer interface
  - LinkedHashMapRowReducer abstract implementation for 90% of cases
  - ResultBearing.reduceRows(RowReducer)
  - @UseRowReducer SQL Object annotation
- Fixed bug in PreparedBatch preventing batches from being reusable.
- Additional Kotlin convenience methods to avoid adding ".java" on every Kotlin type:
  - Jdbi.withExtension(KClass, ExtensionCallback)
  - Jdbi.useExtension(KClass, ExtensionConsumer)
  - Jdbi.withExtensionUnchecked(KClass, callback)
  - Jdbi.useExtensionUnchecked(KClass, callback)
- EnumMapper tries a case insensitive match if there's no exact match
- OracleReturning.returningDml() supports named parameters
- Fixed regression in Postgres typed enum mapper, which caused a fallback on the
  Jdbi default enum mapper.

# 3.0.1
- Kotlin mapper support for @Nested annotation
- ReflectionMapperUtil utility class made public.
- collectInto() and SQL Object return type support for OptionalInt, OptionalLong,
  and OptionalDouble, and Vavr Option.
- New jdbi3-sqlite plugin with SQLite-specific binding and column mapping for java.net.URL.
- Workaround for multithreaded race condition loading StringTemplate STGroups and templates.
- Column mapper for Vavr Option.

# 3.0.0
- [breaking] Added ConfigRegistry parameter to SqlLocator.locate() method.

# 3.0.0-rc2
- Row and column mapper for Optional types
- Binding of nested attributes e.g. ":user.address.city" with bindBean(), bindMethods(),
  bindFields(), as well as @BindBean, @BindMethods, and @BindFields in SQL objects.
- Mapping of nested attributes with BeanMapper, ConstructorMapper, and FieldMapper, using
  the @Nested annotation.
- SQL Objects inherit class annotations from supertypes.
- bindList() and @BindList now follow the parameter naming style of the active SqlParser,
  via the new SqlParser.nameParameter() method. e.g. ":foo" for ColonPrefixSqlParser, vs
  "#foo" for HashPrefixSqlParser.

# 3.0.0-rc1
- SQL Object methods may have a Consumer<T> instead of a return type. See
  http://jdbi.github.io/#_consumer_methods.

# 3.0.0-beta4
- [breaking] ResultSetMapper -> ResultSetScanner; reducing overloaded 'Mapper'
- PreparedBatch: throw an exception if you try to add() an empty binding
- [breaking] Removed column mapper fallback behavior from
  StatementContext.findRowMapperFor() and RowMappers.findFor(), in favor or new
  StatementContext.findMapperFor() and Mappers.findFor() methods. Previously,
  findRowMapperFor() would first consult the RowMappers registry, then the
  ColumnMappers registry if no RowMapper was registered for a given type. Thus:
  - StatementContext.findMapperFor(...) or Mappers.findFor() may return a row mapper or
    a first-column mapper.
  - StatementContext.findRowMapperFor(...) or RowMappers.findFor() returns only row
    mappers
  - StatementContext.findColumnMapperFor(...) or ColumnMappers.findFor() returns only
    column mapper
- [breaking] Renamed @SqlMethodAnnotation meta-annotation to @SqlOperation.
- Added support for Vavr object-functional data types in jdbi3-vavr module.
- java.time.ZoneId support

# 3.0.0-beta3
- Added Kotlin extension methods to Jdbi class, to work around Kotlin's lack
  of support for exception transparency: withHandleUnchecked,
  useHandleUnchecked, inTransactionUnchecked, useTransactionUnchecked,
  withExtensionUnchecked, useExtensionUnchecked.
- Renamed org.jdbi:jdbi3 artifact to org.jdbi:jdbi3-core, for consistency with
  other modules.
- [breaking] StatementContext.getParsedSql() now returns a ParsedSql instead of String
- [breaking] Remove SqlStatement fetchForward / Reverse ; statements now FORWARD_ONLY

# 3.0.0-beta2
- [breaking] Removed Handle.update() and Handle.insert(), in favor of
  Handle.execute(), which does the same thing. Handle.execute() now returns
  the update count.
- Removed core dependency on Guava.
- [breaking] Switch from 1- to 0-based indices in OracleReturning.returnParameters()
- [breaking] Added StatementContext parameter to NamedArgumentFinder.find() method
- [breaking] Moved JoinRowMapper.JoinRow class to top-level class
- [breaking] Modified @Register* annotations to be repeatable, instead of using
  array attributes.
- [breaking] Moved and renamed MapEntryMapper.Config to top-level class
  MapEntryMappers
- MapMapper preserves column ordering, #848
- [breaking] split Handle.cleanupHandle() into cleanupHandleCommit() and *Rollback()
- [breaking] remove TransactionStatus enum
- [breaking] Refactored StatementRewriter into TemplateEngine and SqlParser.

# 3.0.0-beta1
- [breaking] Refactored SqlStatementCustomizerFactory.createForParameter(...)
  - Now returns new SqlStatementParameterCustomizer type, so parameter customizers
    can be cached and reused for performance.
  - Now accepts a `Type` parameter, so parameter binders no longer have to check
    whether the statement is a PreparedBatch.
- [breaking] Handlers config class, refactored HandlerFactory permit alternative
  method handler mapping strategies.
- [breaking] Renamed BeanMapper, FieldMapper, and ConstructorMapper's `of(...)`
  methods to `factory(...)`. Added `of` methods in their place which return
  RowMappers, whereas the `factory` methods from before return `RowMapperFactory`s.
- [breaking] Mixing named and positional parameters in SQL statements will now
  throw an exception. See https://github.com/jdbi/jdbi/pull/787
- Handlers registry allows users to use custom SQL Object method handlers
  without a SQL method annotation.
- HandlerDecorators registry allows adding custom behavior to any SQL Object
  method, with or without an annotation.
- jdbi3-kotlin plugin adds support for mapping Kotlin data classes.
- jdbi3-kotlin-sqlobject plugin adds support for Kotlin SQL Objects,
  including Kotlin default methods, and default parameter values.
- Support for collecting results into Map, and Guava's Multimap.
- Configuration option to control how "untyped null" arguments are bound.
  Useful for some database vendors (e.g. Derby, Sybase) that expect a different
  SQL type constant for null values.
- Support boolean[] return type from @SqlBatch methods
- Bug fixes:
  - NullPointerException in Postgres plugin when binding null UUID
  - IllegalArgumentException with @SqlBatch when the batch is empty
  - NullPointerException when `null` is bound to an array column.

# 3.0.0-beta0
- Redesigned for Java 8 - lambdas, streams, optionals, exception transparency
- Support for java.time (JSR-310) types like LocalDate and OffsetDateTime
- Better support for custom collection types, using Java 8 Collector
- SQL array support -- finally!
- BeanMapper and other reflection-based mappers now recognize snake_case
  column names, and match them up to Java properties
- Plugin architecture that makes it easy to share configuration
- Plugins to support types from 3rd party libraries: JodaTime, Guava,
  StringTemplate, Spring
- Plugins to support specific database vendors: H2, Oracle, Postgres
- Migration-friendly: Jdbi v2 and v3 will happily coexist within the same
  project, so you can migrate at your own pace.

# 2.78
- @BindIn: fix handling of empty lists on Postgres
- clear SqlObject ThreadLocals on close, fixes leak on e.g. webapp reload
- expose Script.getStatements()

# 2.77
- Improved BindIn functionality: can now process Iterables and arrays/varargs
  of any type, and has configurable handling for a null/empty argument.
  Check the source code comments or your IDE hints for details.

# 2.76
- SPRING BREAKING CHANGE: move from Spring 2 to Spring 3, how timely of us
- SQL lookups in the context of a SqlObject method now also find according
  to the same rules as annotation
- DefaultMapper now has option to disable case folding
- Fix AbstractMethodError swallowing in SqlObject methods

# 2.75
- simple @GetGeneratedKeys @SqlBatch support (only int keys for now)
- ClasspathStatementLocator performance improvements

# 2.74
- cglib 3.2.2, asm 5.1; fixes codegen for new Java 8 bridge methods
- @UseStringTemplate3StatementLocator now caches created locators
- new @OutParameter annotation for fetching named out params on @SqlCall methods
- expose Handle.isClosed

# 2.73
- Allow clearing of bindings in SQLStatement
- (finally!) parse Postgres CAST syntax 'value::type' properly in colon
  prefix statements
- fix @SqlBatch hanging if you forget to include an Iterable-like param
- fix @SqlUpdate @GetGeneratedKeys to allow non-number return types
- Expose Foreman on StatementContext

# 2.72
- Support for the ability to provide a list of the column names returned
  in a prepared batch #254

# 2.71
- fix @BindBean of private subtypes, #242

# 2.70
*** MAJOR CHANGES ***
- allow JDK8 default methods in SQLObject interfaces. Backport of #190.
- switch to standard Maven toolchains.xml for cross-compilation, #169.
  See https://maven.apache.org/guides/mini/guide-using-toolchains.html
  for instructions on how to use it.
- Correctly handle semicolons and inline comments in SQL statements.
  Existing SQL statements may break due to lexer changes, ensure you have
  test coverage.
- Introduce "column mappers" which dramatically improve type handling
  for BeanMapper-style automatic mapping.
  See https://github.com/jdbi/jdbi/pull/164
- Disallow "nested" transactions explicitly.  They almost certainly don't
  work the way you expect.  Use savepoints instead.
- Eagerly check return type of @SqlUpdate annotated SqlObject methods
- Allow getting generated keys by name for Oracle
- Allow getting generated keys from prepared Batch statements
- Cache StatementRewriter parsing of statements
- Support mapping of URI, char, Character types

# 2.63
- Include lambda-friendly callback methods on Handle and DBI, #156

# 2.62
- Also include asm in shade, fixes build.  Sorry about the broken releases...

# 2.61
*** DO NOT USE ***
- Fix shading broken in 2.60, fixes #152

# 2.60
*** DO NOT USE ***
- Fix Javadoc generation for JDK6 and JDK8
- Add support for /* */ style comments in statements
- Add @BindMap annotation which allows parameters passed in a Map<String, Object>
- Add support for running Script objects as individual statements rather than batch
- Add support for default bind name based on argument position number (thanks @arteam)
- Fix SqlObject connection leak through result iterator (thanks @pierre)
- Switch to using cglib instead of cglib-nodep so we can pull ASM 5.0.2 which is Java 8 compatible
- Classmate to 1.1.0

# 2.59
- Fixes #137, broken ClasspathStatementLocator cache (thanks @HiJon89).
- Recognize MySQL REPLACE statements

# 2.58
- Identical to 2.57 except that the jar is correctly shaded.

# 2.57
*** DO NOT USE ***
- Packaging for 2.57 was accidentially broken, use 2.58 instead.
  Thanks to @HiJon89 for spotting the problem!
- use Types.NULL for null objects (thanks @christophercurrie)
- improve behavior on transactional autocommit (thanks @hawkan)
- fix connection leak in on-demand sqlobject (thanks @pmaury)
- code cleanups

# 2.54
- fix cleanup bug when e.g. cleanupHandle was called multiple times
  on the same query.
- Generic object binding uses specific type if value is non-null.

# 2.53
- Tests now run in parallel
- Added Template supergroup loading to StringTemplate3StatementLocator
- add a global cache for templates loaded from an annotation.
- fix a handler cache bug.

# 2.52
- not released

# 2.51
- fix PMD, Findbugs and javadoc complaints
- clean license headers in all source files
- use basepom.org standard-oss base pom to build, build with all checkers enabled
- build with antlr 3.4
- use classmate 0.9.0 (from 0.8.0)
- make all dependencies that are not required optional (not provided)

# 2.50
- add travis setup for autobuilds
- Remove log4j dependency for slf4j logger
- Ensure that compilation using JDK7 or better uses a JDK6 rt.jar
- Fix the @BindBean / Foreman.waffle code to use correct ArgumentFactories and not just the ObjectArgumentFactory
- fix spurious test failures when using newer versions of the surefire plugin

# 2.45
- Support for setting Enum values from strings in BeanMapper
  
# 2.44
- Add java.io.Closeable to Handle and ResultIterator
 
# 2.35
- Use CGLIB for sql objects instead of dyanmic proxies
- Support for classes as well as interfaces in the sql object api
- Add @Transaction for non @Sql* methods in sql objects
- @CreateSqlObject annotation sql objects to replace Transmogrifier

# 2.31
- Add access to ResultSet on FoldController

# 2.12
- Registered Mappers on DBi and Handle, and the Query#mapTo addition
- Sql Object API

# 2.11
- Botched release attempt with Maven 3

# 2.10.2
- Bugfix: Allow escaping of arbitrary characters in the SQL source, especially allow
  escaping of ':' (which is needed for postgres type casts)

# 2.10.0
- minor code cleanups to reduce number of warnings
- Expose NamedArgumentFinder to allow custom lookup of Arguments. JDBI already provides
  two implementations of the Interface, one for Maps and one for BeanProperties.
- Add ability to set query timeout (in seconds) on SqlStatement

# 2.9.3
- Add <url /> element to pom so can get into central :-)

# 2.9.2
- Add ` as a legal SQL character in colon prefix grammar
- non-existent release, fighting maven

# 2.9.1
- First 2.9 series release

# 2.9.0
- Make the DefaultMapper public.
- Aborted, trying to make gpg signing work correctly

# 2.8.0
- Add methods to SqlStatement and PreparedBatch that allow adding a set of defines
  to the context in one go.
- Add ~ { and } as legal characters in the colon prefix grammar

# 2.7.0
- A TimingCollector was added which can be registered on the DBI or handle which then
  gets called with nanosecond resolution elapsed time every time a statement is run
  against the data base.
- re-added some Exception constructors that were accidentially removed in 2.3.0 making
  2.4.0-2.6.x non-backwards compatible.
- Bind java.util.Date as a timestamp because it contains time and date.
- BasicHandle constructor is now package private (which it always should have been)
- add Clirr Report to the Maven Site
- convert all calls to System.currentTimeMillis() to System.nanoTime(), which is more
  accurate and much more lightweight. As we only calculate time differences, it is
  good enough.
- fix more compiler warnings
- add null checks for all object types on SqlStatement
- move object null checks, that don't require boxing/unboxing
  into the Argument classes. Keep the checks for object/primitive
  types in SQL to avoid boxing/unboxing overhead.

# 2.6.0
- Fix a number of compiler warnings
- Add new binding methods for SqlStatement
  - Integer, Boolean, Byte, Long, Short  Object
  - double, float, short primitive
- All bind methods taking an object should check for null values and bind a NullArgument accordingly.

# 2.5.0
- Add new binding methods for SqlStatement
  - char types
  - boolean as int (for DBs missing a boolean type)
- Re-add unit test removed in 2.4.9 with unicode escapes

# 2.4.9
- Remove Unit tests that fails depending on Platform Encoding

# 2.4.8
- Switch to ANTLR 3 for grammars so that shading works again

# 2.4.5
- Move source code to github

# 2.4.4
- Fix several dependency and shading issues which came up from the
ant to conversion.

# 2.4.3
- Add better messages on statement exceptions

# 2.4.2
- Switch to maven2 for builds
- Add the statement context to statement related exceptions, including a new
  DBIExcpetion abstact subclass, StatementException, which exposes this.

# 2.3.0
- Fix OracleReturning compile time dependency using Reflection.
- Deprecated OracleReturning.
- Added CallableStatement support :
  - new method handle.prepareCall
  - new Call class and CallableStatementMapper interface
- Fixes to colon prefix grammar to support empty string literals and escaped quotes.
- Added access to more of the actual context for a statement to StatementContext

# 2.2.2
- Change OracleReturning to use oracle.jdbc.oraclePreparedStatement for
  compatibility with ojdbc6.jar compatibility

# 2.2.1
- Fix a result set leak in the case of a Mapper raising an exception rather
than returning cleanly

# 2.2.0
- Add DBI#inTransaction

# 2.1.1
- Add timing info to logging calls

# 2.1.0
- Add Query#fold
- Add additional logging around handles and transactions

# 2.0.2
- Clean up a NullPointerException which was masking an UnableToCreateStatementException

# 2.0.1
- Add '!' to the characters for LITERAL in the colon prefix grammar

# 2.0.0
- Add Query#list(int) in order to allow for a maximum resukt size from eager query execution.
- Add sql logging facility

# 1.4.6
- Fix an NPE when dealing with metadata in Args.

# 2.0pre17
- Change statement customizer to have before and after callbacks
- Change OracleReturning to use the after callback to extract results

# 2.0pre16
- Clean up the build so the stringtemplate stuff is useful
- SqlStatement#bind(*, Character) which converts to a string
- Provide a non-caching default statement builder
- Allow setting the statement builder on a DBI explicitely
- Allow re-use of a prepared batch by clearing the parts prior to execution
- Change query iterated results to clean resources in the same manner as list, just later

# 2.0pre15
- Move StringTemplate stuff back into unstable
- Support for checkpointed transactions

# 2.0pre14
- Add convenience classes for one value result sets
- StringTemplate 3.0 based statement locator and a classpath based loader
- Improve grammar for named param parsing (| in LITERAL)

# 2.0pre13
- Spring (2.0) support classes
- Add ability to define statement attributes at the DBI and Handle levels
- Have prepared batch use the statement locator
- Bean resultset mapper invokes the right ResultSet.getXXX() for each
  property type (getObject() on Oracle returns internal Oracle types)
- Allow positional binding for PreparedBatch
- Renamed PreparedBatchPart.another() to next()
- Change SqlStatement#first to return null on an empty result instead of an NPE
- Allow setting attributes on statement contexts for batches and prepared batches
- SqlStatement.bindNull(...)

# 2.0pre12
- [bugfix] Pass statement context into result mapped queries

# 2.0pre11
- Create the StatementContext to allow for tunneling state into the various
client defined tweakables

# 2.0pre10
- allow numbers in named params

# 2.0pre9
- Fix up IDBI to have the DBI functional methods and not the config methods

# 2.0pre8
- Add double quote handling to named param magic

# 2.0pre7
- Added Oracle DML Returning features

# 2.0pre6
- Pluggable statement builders
- More literal characters in the named statement parser

# 2.0pre5
- Improve grammar for named param parsing (_ @ and _ in LITERAL)

# 2.0pre4
- Switch to an ANTLR based grammar for named param parsing

# 2.0pre3
- JDBC4 Style "Ease of Development" and API Docs

# 2.0pre2
- Flesh out convenience APIS

# 2.0pre1
- Complete Rewrite

# 1.4.5
- Fix bug in caching added in 1.4.4
- Optimize statement literal or named statement detection

# 1.4.4
- Allow for create/drop/alter statements
- Cache whether or not a driver supports asking for prepared statement parameter types

# 1.4.3
- Handle drivers (such as Oracle) which throw an exception when trying to retrieve
  prepared statement parameter type information.

# 1.4.2
- Be explicit about target jdk version (1.4) for this branch

# 1.4.1
- Fixed bug where null is being set via setObject instead of setNull. Thank you, Simone Gianni!

# 1.4.0
- Expose the new functionality on interfaces as well as concrete classes

# 1.3.3
- Expose the handle decorator functionality on the IDBI interface
- Add a script locator mechanism analogous to the statement locator

# 1.3.2
- Save SQLException to provide more information to the DBIException on statement execution

# 1.3.1
- Issue with a matcher not being reset which only showed up under jdk 1.5. Thank you Patrick!

# 1.3.0
- Wrap exceptions thrown from handle in Spring DataAccessExceptions for the
  Spring adaptor. Thank you Thomas Risberg.
- Support for "global" named parameters at the handle and DBI levels

# 1.2.5
- Change Handle#script to batch the statements in the script

# 1.2.4
- Bug fix in named parameter handling with quotes (would ignore some named params incorrectly)

# 1.2.3
- Allow configuring transaction handlers in properties
- Allow configuring of externalized sql locating (ie, non-classpath)

# 1.2.2
- Add callback based transaction handling in order to cleanly support the various
  transactional contexts (CMT, BMT, Spring, Local) etc.

# 1.2.1
- Via the Spring DBIBean, IDBI#open(HandleCallback) now uses the transactionally bound handle
  if there is one.

# 1.2.0
- DBIException now extends RuntimeException. The 7 character change major release =)
- Added DBIUtils.closeHandleIfNecessary(Handle, IDBI) to allow for transparently managing
  transactions and connections in Spring whteher tx's are enabled or not.

# 1.1.2
- Handle#query(String, RowCallback): void no longer starts a transaction automagically

# 1.1.1
- Support full-line comments in external sql and sql scripts. Full line comments
  must begin with # or // or -- as the first character(s) on the line.

# 1.1.0
- Added handle#first(..): Map convenience functions to query for individual rows
- Removed DBITransactionFailedException and used plain old DBIException in its place
- Added unstable package for holding elements subject to API changes during a major release cycle.
- Handle decorator functionality added to unstable feature set
- JavaBean mapped named parameter support
- Renamed Handle#preparedBatch to Handle#prepareBatch
- Queries return java.util.List instead of java.util.Collection
- Much more sophisticated auto-configuration
- Broke backwards compatibility on handle.query(String, Object) method behavior
  (this is reason why 1.1.0 version increment)
  (read the javadocs if you use this method)
- Removed method Handle#query(String, Object, Object). Could lead to confusion with changed behavior mentioned above

# 1.0.10
- Batch and PreparedBatch Support
- Removed an unused exception
- Fixed bug in named parameter extractor (would miss named params not preceeded by whitespace)

# 1.0.9
- Better auto-detection of statement type (named, raw sql, etc)

# 1.0.8
- Spring integration tools

# 1.0.7
- Provide an interface for the DBI class in order to play nicer with proxies

# 1.0.6
- Prepared statement re-use was failing on Oracle, fixed.

# 1.0.5
- Fleshed out the execute(..) methods to take full array of arguments, like query.
- Added update(..): int which return number of rows affected
- Lots of internal refactoring

# 1.0.4
- Was swallowing an exception in one place for the (brief) 1.0.3 release. Definitely upgrade if using 1.0.3

# 1.0.3
- Fixed a bug where quoted text could be interpreted as named tokens, bad me.
- Added HandleCallback methods to DBI to manage handle db resources etc for clients.
- Removed test dependency on Jakarta commons-io, which had been used, previously,
  for deleting the test database. Tests now depend only on derby and junit, still
  with no runtime dependencies (other than the JDBC driver for your database).

# 1.0.2
- Added facility for loading connection info from properties file
  for convenience. Totally optional, thankfully.

# 1.0.1
- Added overloaded argument signatures to callback-based queries

# 1.0
- Initial Release
