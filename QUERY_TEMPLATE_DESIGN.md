# QueryTemplate — design & implementation plan

**Status:** working document. This is a living tasklist. As each piece lands, its
substance moves into javadoc / the user guide where it belongs. When the feature
is complete this file should be redundant and gets **deleted**. If this file still
exists, the work is not done.

Target branch: `query-templates` (on top of `jdbi4-dev`).

## Goal

Performance. High-frequency statement execution (e.g. 10k small statements against
a local database) wastes CPU and allocations on two things done *per statement*:

1. `ConfigRegistry.createCopy()` — `BaseStatement` deep-copies every registered
   config object (Arguments, all Mappers, SqlArrayTypes, …) for every statement,
   purely to isolate per-statement mutable state.
2. Re-wiring — re-parsing and re-rendering SQL and rebuilding the execution
   scaffolding on every call.

A `QueryTemplate` does this work **once** and reuses it across executions and
threads. SQL Object is the flagship consumer: each `@SqlQuery` method has fixed
SQL known at proxy-build time, so it builds one template per method and only
binds + executes per invocation.

## Core model

- **Config is immutable and shared.** It is snapshotted once at explicit
  boundaries (Jdbi setup, Handle construction, template build) and never copied
  per statement. Mutation happens only inside a `configure(callback)` scope;
  `build()` takes a single snapshot. A retained mutable reference cannot affect a
  built template (the template holds its own snapshot), so there is no
  `IllegalStateException("read only")` pitfall — the type/lifecycle carries the
  guarantee, not a runtime flag.
- **Defines are separate from config.** They are a small per-render attribute map,
  not part of the heavyweight registry. A template captures a constant set at
  build time and renders eagerly; an execution may supply a tiny overlay. Empty
  overlay → reuse the pre-rendered SQL; non-empty → re-render via the retained
  parsed render function (`TemplateEngine.Parsing.parse` already returns a
  reusable `Function<ConfigRegistry, String>`).
- **Statement state lives on the statement, not in config.** Per-execution mutable
  state (bound `Binding`, rendered SQL, JDBC `Statement`, cleanables, fetch size,
  `returningGeneratedKeys`, `concurrentUpdatable`, `mappedRows`, `traceId`) moves
  onto the per-execution object. This is what removes the *reason* config was
  copied per statement.
- **Reconfigure derives a new template.** No in-place mutation. Either build a
  separate template, or `reconfigure(callback)` which backs up to a fresh mutable
  copy, applies changes, and rebuilds into a new immutable template.
- **Caches unwind.** With immutable config, resolution ("what mapper handles type
  T") is a pure function of the config instance and can be memoized *on that
  instance* (eagerly at build, or a per-instance thread-safe cache) instead of in
  a global, mutable, config-entangled cache. The template holds its own parsed
  render function, so the `SqlStatements` template cache buys it nothing. The only
  remaining candidate for a shared cache is naive repeated *classic* usage, and if
  we keep anything there it is a single pure `SQL text → parsed function`
  concurrent memoization, decoupled from config — not what exists today.
- **One unified path.** The fluent `Handle.createQuery`/`Update`/… API and SQL
  Object are both reimplemented on the template primitive. Nothing is kept as a
  slow deprecated wrapper.

## Field taxonomy (reference)

Every field currently in `StatementContext` / `SqlStatements` / config gets a home.
Inventoried from the actual sources:

**`SqlStatements` (today a config object, deep-copied per statement):**

| Field | Destination |
| --- | --- |
| `templateEngine`, `sqlParser`, `sqlLogger` | **config** (immutable) |
| `exceptionHandlers`, `contextListeners` | **config** (immutable) |
| `allowUnusedBindings`, `scriptStatementsNeedSemicolon` | **config** (policy) |
| `attachAllStatementsForCleanup`, `attachCallbackStatementsForCleanup` | **config** (cleanup policy) |
| `jfrSqlMaxLength`, `jfrParamMaxLength`, `includeBindingsInTelemetry` | **config** (telemetry policy) |
| mappers / arguments / array types / collectors / qualifiers (other config objects) | **config** (immutable) |
| `attributes` (`Map<String,Object>`, read by `DefinedAttributeTemplateEngine`) | **defines** (per-render) — the reason to split defines out |
| `customizers` (`Collection<StatementCustomizer>`) | **config** default set; per-execution additions → **statement state** |
| `queryTimeout` | **config** default; per-execution override → **statement state** |
| `templateCache` | **removed** (cache unwinding, phase 4) |

**`StatementContext` (per-execution today, but owns a copied config):**

| Field | Destination |
| --- | --- |
| `config` | reference to the **immutable** config (no longer a per-statement copy) |
| `extensionMethod`, `jdbiStatementType` | **statement state** |
| `rawSql`, `renderedSql`, `parsedSql` | **statement state** (rendered/parsed hoistable to the template for constant defines) |
| `statement` (`PreparedStatement`), `connection` | **statement state** |
| `returningGeneratedKeys`, `generatedKeysColumnNames`, `concurrentUpdatable` | **statement state** |
| `executionMoment`, `completionMoment`, `exceptionMoment` | **statement state** (timing) |
| `mappedRows`, `traceId` | **statement state** (instrumentation) |
| fetch size, max rows, max field size (via `StatementCustomizers`) | **statement state** |
| `extensionState` (opaque, `@Alpha`; added 2026-07-17) | **statement state** (per-execution) — the extension layer's per-invocation holder (SQL Object's args/returner), replacing the deleted `SqlObjectStatementConfiguration` |

Post-retarget (2026-07-17) reality already realized on the template path: per-execution
customizer additions and `queryTimeout` overrides live on the `QueryTemplateBinding` (its
local `customizers` list), not on the shared config; `attributes` are the per-render defines
overlay. What phase 2 still owes: making the *config itself* immutable after build (so the
classic path also skips the copy) and turning an un-marked config-mutating customizer's write
into a loud error instead of the current silent shared-snapshot mutation (see the "Customizer phase model" section).

## Resolved decisions

- Handle config is **immutable after construction** (opens memoization + cleanup;
  lets the classic wrapper path also skip the copy).
- Per-execution registration: **not supported in place**. Use a separate template
  or `reconfigure(callback)`.
- SQL Object: **retargeted onto the primitive** (flagship beneficiary).
- Classic fluent API: **reimplemented on the primitive**, one build per one-shot
  query. Not a slow deprecated layer.
- Thread model: `QueryTemplate` is shared/immutable/thread-safe; a
  `QueryTemplateBinding` is per-execution and thread-confined. `.with(handle)`
  returns a fresh binding per call.

## Customizer phase model — the fix for config mutation (IMPLEMENTED; live on the SQL Object path)

The SQL Object retarget is NOT decoupled from the config redesign after all: statement
customizers can mutate config (e.g. `@AllowUnusedBindings` → `getConfig(SqlStatements).set...`),
and the retargeted query path shares one immutable config snapshot, so per-execution config
mutation would race / leak. Config mutation is uninterceptable today (`ConfigRegistry.get(X)`
returns a shared instance mutated by a plain setter; `StatementContext.config` is final, created
before customizers run), so copy-on-write is not available without the phase-2 redesign.

Resolution (decided with the maintainer) — a **customizer phase model**, applied to the query
path first:

- **Configure (early)** — type/method-level, invariant. Applied **once** when the template is
  built, against the template's method-level config. Config mutations and registered statement
  customizers freeze into the shared snapshot. Free per invocation. Built-ins: `@AllowUnusedBindings`,
  `@DefineNamedBindings`, method-level `@FetchSize`/`@MaxRows`/`@QueryTimeout`, reworked `@Timestamped`.
- **Bind** — parameter-level (+ the default per-param binder). Applied per invocation to the
  binding; only `bind`/`define`, never config. Free (no config copy). Built-ins: `@Bind*`,
  `@Define*`, `@MapTo`, param `@FetchSize`/`@MaxRows`.
- **Late** — opt-in escape hatch (marker interface `ConfigMutating`) for a customizer that must
  mutate config per invocation. A method carrying any late customizer falls back to the classic
  per-statement `Query` path (config copy per call = existing correct behavior). No built-in needs it.

Default policy: fast by default (type/method → configure, parameter → bind); late is explicit
opt-in ("opt into the expensive path"). A customizer that mutates config per invocation MUST
declare itself late, else it silently mutates the shared snapshot (a phase-2 immutable config
would turn this into a loud error; deferred). Consequences accepted: (1) method/type-level
`SqlStatementCustomizer`s are now build-time — a method-level customizer that binds per
invocation must be a parameter customizer or register a `StatementCustomizer`; (2) `@Timestamped`
is reworked from eager `bind(now())` to a configure-registered `StatementCustomizer` binding a
fresh `now()` in `beforeBinding` each execution.

Implementation notes: base `CustomizingStatementHandler` becomes non-generic over `Customizable<?>`
(so `SqlQueryHandler.createStatement` can return a `Query` for the late/classic path or a
`QueryTemplateBinding` for the fast path); per-invocation `args`/`returner` move off
`SqlObjectStatementConfiguration` (deleted) onto an opaque `@Alpha` `StatementContext` slot.

## HANDOFF (2026-07-19): the whole feature is implemented — phase-2 through D6, and REMOVAL R1–R7 + R5-D + R6 are all DONE, whole reactor green. Remaining work is release-time only (see "What actually remains" below). **LATEST: see "## SESSION 2026-07-20" — the phase-5 engine unification is DONE (one `SqlStatement.internalExecute` for classic + template, `QueryTemplateBinding` deleted, JFR now on the template path, `ConfigRegistry` lazy maps) and `QueryTemplate` is renamed to `StatementTemplate` with terminal-picks-kind (query or update from `with(handle)`) plus `call(handle)`/`prepareBatch(handle)` accessors so one template materializes as any parameterized kind (Tier 1 DONE). Tier 2 (SQL Object `@SqlUpdate`/`@SqlCall`/`@SqlBatch` adopting templates) is now also DONE (2026-07-20, see the SESSION 2026-07-20 section, Tier 2 bullet — benchmark-validated: modest-but-real per-op update win, no batch regression). All four parameterized SQL Object statement kinds share one template engine. Whole core+sqlobject+benchmark reactor green with checks + tests. The feature is now fully implemented; only release-time work remains (see "What actually remains"). The prior "## SESSION 2026-07-19b" recommended this unification.**

> **REMOVAL PROGRESS:** R1 (`60014fa30`) + R2 (`2c9c4aa6f`) + R3 (`640246519`, review `09e26deca`) + R7
> (`0b3dbf9a1`) DONE, whole reactor green. **R3 landed the #2992 handle-COW payoff** (`Handle` config =
> `createChild()` off the frozen root) AND — the key discovery — moved the per-callback attach-for-cleanup scope off
> per-handle config onto a `Handle.forceAttachStatements` flag, because `withHandle`'s
> `configure(SqlStatements, attachAll=attachCallback)` was forking every callback handle and negating the COW
> sharing for the fluent path. **R7 quantified it** (new `HandlePerOpV3Benchmark`, handle-per-op): SQL Object attach
> **134.6 → 45.6 KB/op (−66%, 2.95×), 1.3× thrpt**; `withHandle` matches `open` (proving the flag fix); fluent −10%;
> no regression on warm-handle paths (classic 4208 / template 3512 unchanged). See the R3 + R7 bullets.
> **R4 DONE (2026-07-19, `1abd58572`, whole reactor green — 4860 tests, 0 failures, checks clean).** `Handle`
> implements read-only `ConfigReader`; the `JdbiPlugin.customizeHandleConfig(Connection, ConfigRegistry)` construction-
> time SPI (OPTION 2) replaced `PostgresPlugin.customizeHandle`'s config mutation with no escape hatch; 92 test files
> migrated (core + 18 downstream) via `withConfig` / statement-level / scoped `open`/`useHandle`, run as two workflow
> fan-outs (core-test then downstream) with a hard serialization point at core-test. **NEXT: R5** (final `Configurable`
> read/mutate split cleanup — see the TRUE-IMMUTABILITY note on the R4 bullet: make read-only contexts expose only
> read methods so a discarded-wither / dead `configure()` can't compile), then **R6** (upgrade-guide docs).
> **R5 DONE (2026-07-19): R5-A `dc169f3ef` (read-only `ConfigView` delegate — `getConfig().configure()` won't
> compile and the returned view can't be cast to `ConfigRegistry`), R5-B `992df4f67` (`@CheckReturnValue` on ~90
> config withers so a discarded wither fails spotbugs). No hot-path regression. **R5-D DONE (2026-07-19, whole reactor green): extended pure Option 3 — narrowed the whole read-only resolve path (7 factory SPIs + Preparable.prepare + init + ExtensionFactory getters + PojoPropertiesFactory chain + ConfigCache + Codec + JsonMapper family) to `ConfigView`; `readAs` hands a cached `asReadOnlyView()`, closing the leak as a side effect; 138 files/17 modules. See the R5-D bullet.** **R6 (upgrade guide) DONE (2026-07-19): see the "## Upgrading to Jdbi v4" section — publication-ready, with verified before/after benchmarks; lift into `docs/src/adoc/index.adoc` at release.** **R2 made `Jdbi` read-only**
> (`implements ConfigReader`, removed `installPlugin`/`setX` knobs/`JdbiPlugin.customizeJdbi`), migrated ~150 test
> sites + the cache/kotlin/guice/spring/examples main-source consumers, and added the test-extension conveniences
> `withConfig(Consumer<Jdbi.Builder>)` + `builder()`. The Jdbi root config is now frozen after `build()`, so **R3
> (handle `createCopy`→`createChild` COW = the jdbi/jdbi#2992 warm-metadata payoff) is UNBLOCKED and is next.**
> Small R2 polish still pending (re-point clear-win `Jdbi.builder(ext.getUrl())` rebuilds → `ext.builder()`; leave
> isolation-intent + doc snippets bare). Details in the "REMOVAL phase" section.

> **VERSIONING FRAMING (2026-07-18, maintainer-confirmed).** This branch **is** the next major (jdbi 4.x, still
> `4.0.0-SNAPSHOT`, unreleased). Earlier phases wrote "removal in the next major" as if removal were a *future*
> release gated behind a 4.x deprecation window — that framing is WRONG. There is no separate future major to wait
> for: the deprecated mutation surface is pre-release scaffolding, and **removal happens in THIS branch, now.** The
> `@Deprecated(forRemoval=true)` markers from D4b.2 were a staging convenience, not a compat promise to external 4.x
> users (4.0 has not shipped). So "at removal" / "next major (removal…)" throughout this doc means **this branch's
> removal phase**, which is the next work item. The `@Deprecated` markers are deleted along with the methods they mark.

**Latest (2026-07-18):** **D4b.1 DONE** (`674f6c496`, `661334b55`). All 17 in-repo plugins moved their config from
`customizeJdbi(Jdbi)` to `configure(Jdbi.Builder)`; `Jdbi.installPlugin` now bridges through a throwaway
`Builder` (`new Builder(this).installPlugin(p).build()`) so every existing install site keeps working with **no
lockstep call-site migration** — this revises the doc's original "pure move + migrate ~47 sites" plan (which would
have silently broken the direct-install path during the window). `build()` and `installPlugin` share a private
`applyPlugin(Builder, plugin)` funnel; `build()` drains plugins by index; added `JdbiPlugin.of(Consumer<Builder>)`.
The bridge is disposable at removal. Details + the corrected migration plan are in "## D4b proposal" below.
**D4b.2 DONE (`8f69b9f4e`):** deprecated the DIRECTLY-OWNED mutation surface (`Jdbi.installPlugin`/`setTransactionHandler`/
`setStatementBuilderFactory`/`setHandleCallbackDecorator`/`setHandleScope` + `JdbiPlugin.customizeJdbi`,
`@Deprecated(since="4.0.0", forRemoval=true)`); routed central test support (`JdbiExtension` + core-test H2/Pg/Sqlite
`DatabaseExtension`) through the builder. Two maintainer-confirmed scope calls: the `Configurable` mutators inherited by
`Jdbi` (`configure`/`register*`) are **NOT** deprecated now — deferred to removal (avoids ~6 override-stubs + a warning
flood; they go when `Jdbi` drops `Configurable`'s mutators); and javac deprecation warnings are non-fatal + already
pervasive in-repo, so ~66 incidental leaf callers are left to warn (swept at removal), only `TestJdbiBuilder`/`TestPlugins`
(subject = deprecated path) get `@SuppressWarnings`. Earlier: D4a (`d5565bf13`) landed the additive `Jdbi.builder()`
assembly API + the `JdbiPlugin.configure` SPI (D5).
**D6 window slice DONE:** added the additive `@Alpha` `Jdbi.open(Consumer<ConfigRegistry> configScope)` + scoped
`withHandle`/`useHandle`/`inTransaction`/`useTransaction` — a handle opened with a scope carries a `createCopy()` of
the Jdbi config with the scope applied (in place, before the extension context + `Handles` listeners are derived).
The internals are a facade (`createCopy`+apply); the removal-gated pieces are the REMOVAL phase below.

### REMOVAL phase (this 4.x branch) — the plan [maintainer-confirmed 2026-07-18]
The largest, most breaking phase; it lands the jdbi/jdbi#2992 warm-metadata perf prize. Sequenced to deliver the
prize early, then finish the handle-config removal:
- **R1 — migrate in-repo Jdbi-mutation call sites to the builder** (behavior-preserving prep). ~90–100 sites across
  ~59 files: `Jdbi.create(x).registerY(...).installPlugin(p)` / post-`create` `jdbi.registerY(...)` →
  `Jdbi.builder(x).registerY(...).installPlugin(p)…build()`. Central test support already done (D4b.2).
- **R2 — remove the Jdbi mutation API; `Jdbi` becomes read-only. [DONE `2c9c4aa6f`, whole reactor green. Details in
  the branch memory [[query-templates-branch]] "R2 DONE" entry — including the `withConfig(Consumer<Builder>)` +
  `builder()` test conveniences, the JdbiJtaTest discard-mutation fix, and two gotchas: immutable-config
  `getConfig(X).<wither>()` silently discards, and agents' `test-compile` skips checkstyle. Pending polish = re-point
  clear-win `Jdbi.builder(ext.getUrl())` rebuilds to `ext.builder()`.]** Delete `installPlugin`/`setTransactionHandler`/
  `setStatementBuilderFactory`/`setHandleCallbackDecorator`/`setHandleScope` + `JdbiPlugin.customizeJdbi` (+ the
  `applyPlugin` bridge's `customizeJdbi` leg; Builder knob setters now write `jdbi` fields directly); `Jdbi implements
  ConfigReader` (read, `org.jdbi.core.statement.ConfigReader`) not `Configurable` (read+mutate). Also migrate
  `KotlinPlugin.kt` + `KotlinSqlObjectPlugin.kt` `customizeJdbi`→`configure(Builder)` (D4b.1 `.java`-grep missed them).
  The builder keeps mutating the `ConfigRegistry` directly during assembly, so this only removes *post-build* Jdbi
  mutation. Root config is then **convention-frozen** (no freeze-flag — consistent with the D4b "no freeze-on-open"
  decision): nothing idiomatic mutates the root post-build; handles/statements fork COW children.
  - **STATUS (2026-07-18):** the main-source API removal is DONE and VALIDATED (whole reactor's `src/main` compiles;
    all breakage is test call-sites) — parked in `git stash@{0}` ("R2 API removal…"). Restore it to resume.
  - **Compiler-driven site list:** removing the API breaks ~150+ test call-sites (37 core test files + downstream
    not yet reached). Dominant pattern: `extension.getJdbi().registerX(m)…useHandle/withHandle(h→…)` (a shared Jdbi
    configured per test method) — exactly what R3's COW makes unsafe, so it genuinely must move.
  - **MIGRATION PATTERN (maintainer-DECIDED 2026-07-18): extension-level config.** Add a
    `withConfig(Consumer<Jdbi.Builder>)` convenience to the test extensions (core-test `DatabaseExtension` interface +
    testing `JdbiExtension`), implemented as `withPlugin(JdbiPlugin.of(consumer))` (reuses the D4b.1 `JdbiPlugin.of`).
    Then move each test's `getJdbi().registerX(m)` onto the extension field: `.withConfig(b → b.registerColumnMapper(m))`,
    stripping the registration from the method (config applies at build, before the shared handle opens). Per-method
    config becomes class-scoped — fine for the static-fixture majority.
  - **Special-case tail (needs judgment, not the field pattern):** `getJdbi().setTransactionHandler(spy)` with a
    per-test Mockito spy (`TestHandle`, `JdbiOpenLeakTest`, `TestTransactions`, sqlobject `TestSqlObjectTransactions`)
    → rebuild a `Jdbi` from the extension's connection source with `.transactionHandler(spy)`, or restructure;
    `customizeJdbi` test-plugins (`TestIterator`/`TestStream`/`TestJdbiNestedCallBehavior`/`TestExtensionContext`/
    `ImmutablesTest`/`TestBindProperties`) → `configure(Builder)`; `oracle12/TestOraclePlugin` asserts config
    before/after `installPlugin` → restructure to `builder().installPlugin().build()` (or drop the before-assert);
    `TestJdbiBuilder`/`TestPlugins` (D4b.2 `@SuppressWarnings`) subject was the removed install path → rework/remove.
- **R3 — handle `createCopy()` → `createChild()` COW off the frozen Jdbi config. [DONE `640246519`, whole reactor
  green with checks.]** `Handle` ctor derives its config as `jdbi.getConfig().createChild()` (both `open()` and
  `open(scope)`); an unmodified handle shares the root's warm resolver `views` (mappers/arguments/extension metadata)
  instead of paying a cold copy, and only forks when the handle's config actually changes. **This is the #2992 fix.**
  Safe because R2 froze the root. Quantify in R7.
  - **KEY DISCOVERY — the one-liner alone did NOT reach the fluent path.** `withHandle`/`useHandle`/`inTransaction`/
    `useTransaction` ran `h.configure(SqlStatements, attachAll = attachCallback)`, and since
    `attachCallbackStatementsForCleanup` **defaults true**, that flipped `attachAll` false→true and forked EVERY
    callback handle off the root (a fork does the same full config copy as `createCopy`), negating the COW win for
    the dominant path (only raw `jdbi.open()`/on-demand-`attach` via `LazyHandleSupplier.getHandle`→`jdbi.open()`
    benefited). **Fix (maintainer-directed): move the per-callback attach scope off config onto the handle.** New
    `Handle.forceAttachStatements` (a PLAIN field — a Handle wraps a JDBC Connection and is thread-confined like its
    other mutable fields, so no volatile; spotbugs `AT_STALE_THREAD_WRITE_OF_PRIMITIVE` suppressed on the setter with
    the thread-confinement justification) set by the callback methods from the `attachCallback` policy at callback
    start; `BaseStatement` attaches when
    `handle.isAttachStatementsForCleanup()` (the flag OR `SqlStatements.attachAllStatementsForCleanup`, still read
    live so post-open `handle.configure(attachAll)` — e.g. `LeakTest` — keeps working). No config write in the
    callback ⇒ no fork ⇒ the fluent path now shares the root's warm views too. Side benefit resolving the earlier
    "subtlety": `getConfig().isAttachAllStatementsForCleanup()` inside a callback now reflects the user's real policy,
    not the transient override. Tests: `TestMapperInit` (unmodified 2nd handle no longer re-inits; added a
    config-changing-handle-forks-and-re-inits case), `TestPreparedArguments` (re-fetch resolver after the config
    change — a resolver is scoped to the registry it came from, and the change forks). **Doc debt for R6:** the
    `index.adoc` §"attaching statements" example (`handle.getConfig(SqlStatements.class).setAttachCallbackStatementsForCleanup(false)`
    inside a callback) is already a discard-mutation no-op under immutable config (pre-existing D-phase debt), and the
    `setAttachAllStatementsForCleanup`/`setAttach…` spellings predate the prefix-free withers.
- **R7 — benchmark + quantify the gains. [DONE 2026-07-18, `0b3dbf9a1`.]** Added `HandlePerOpV3Benchmark`
  (opens a FRESH handle per invocation — the #2992 "one handle per request" scenario — for the fluent and SQL
  Object attach paths, via both `open()` and `withHandle()`); the existing benchmarks all reuse ONE warm handle so
  none exercised the handle-open boundary. A/B = same build with the R3 source (Handle/Jdbi/BaseStatement) reverted
  to `640246519^` vs current, `-prof gc`, 2 forks × 10 iterations. **Results (gc.alloc.rate.norm, R3 out → in):**
  - `openAttachSelect` (SQL Object, the #2992 case): **134580 → 45584 B/op (−66%, 2.95×); 7.2 → 9.4 ops/ms (1.31×)**
  - `withHandleAttachSelect`: **135124 → 46111 B/op (−66%, 2.93×)** — matches `open`, proving the attach-flag-off-config
    fix (without it the callback handle would still fork and stay at ~135 KB)
  - `openFluentSelect`: 13728 → 12302 B/op (−10.4%); `withHandleFluentSelect`: 14080 → 12575 B/op (−10.7%) — smaller
    because the fluent path's alloc is dominated by SQL render/parse + JDBC, and it resolves only the mapper through
    the warm cache (attach also reuses the warm `ExtensionMetadata` resolver, worth ~89 KB/op). Throughput flat.
  - **No regression** on the warm-handle paths: `QueryTemplateBenchmark.classic` 4208 / `template` 3512 B/op
    (unchanged from the D7 baseline); `H2SqlObjectV3Benchmark.attach` (warm reused handle) 30092 B/op, logically
    unaffected (single handle → no per-handle re-warming). The ~89 KB/op the cold attach saves is exactly the
    ExtensionMetadata resolution that a fresh handle no longer repeats.
- **R4 — remove handle-level mutable config. [DONE 2026-07-19, `1abd58572`, whole reactor green — 41 modules,
  4860 tests, 0 failures, static analysis clean.]** `Handle` drops `Configurable`'s mutators → `implements
  ConfigReader` (read-only, like `Jdbi` after R2). Landed exactly as scoped below: 5 main-source files (Handle,
  Jdbi, JdbiPlugin, PostgresPlugin, a ResultIterable javadoc fix) + 92 test files across core and 18 downstream
  modules. OPTION 2 shipped: the new `JdbiPlugin.customizeHandleConfig(Connection, ConfigRegistry)` SPI is applied
  in the `Handle` ctor (after the caller scope, before the extension context); `PostgresPlugin` moved `PostgresTypes`
  there and `VectorEnabler` to `customizeConnection` — no escape hatch. Test migration used the decision tree below
  (setUp→`withConfig`, method-local→statement-level or scoped `open`/`useHandle`). **Gotchas that bit during
  execution:** (1) javac caps errors ~100, so the first core-test scan under-counted 29 files/200 sites — the real
  set was 46 files once earlier packages compiled (always re-scan after fixing a batch); (2) a non-clean core build
  left stale annotation-processor output → spurious `ImmutablesTest` "Couldn't locate ImmutableTrain" (ALWAYS `clean`
  for core, as the build note says); (3) `TestRegisterJoinRowMapper` reuses `JoinRowMapperTest#setUp` with an
  injected extension — hoisting that setUp's mapper registration to the field (rule A) silently broke the reuse;
  it compiled and only failed at test-run, so the full-reactor test run is the essential gate, not test-compile.
  Scope notes (as-built) below.
  <br>**[original scoping, as-built]**
  - **API change is trivial:** `Handle implements Closeable, Configurable<Handle>` → `implements Closeable,
    ConfigReader` (drop `import ...config.Configurable`, add `import org.jdbi.core.statement.ConfigReader`). `Handle`
    has NO self-mutation, NO `Configurable` overrides, and nothing treats it as `Configurable<?>`; `getConfig()` /
    `getConfig(Class)` stay (they live on `ConfigReader`).
  - **MEASURED scope (NOT the stale "~120"): core = 200 sites / 37 files (compiler-exact); full reactor ≈ 400–430
    sites / ~70 files.** grep UNDER-counts badly (core 107 grep vs 200 real, ≈1.87×). Per-module ≈: kotlin ~65,
    kotlin-sqlobject ~41, sqlobject ~34, docs ~24 (adoc), vavr ~21, stringtemplate4 ~8, 1–4 each across
    json/jackson2/jackson3/gson2/moshi/guava/freemarker/generator/opentelemetry/testcontainers/postgres/
    noparameters/e2e.
  - **ONLY ONE main-source break: `PostgresPlugin.customizeHandle` (`postgres/.../PostgresPlugin.java:177`).** It
    legitimately needs per-connection config: PG types applied to the connection + a `PgLobApiImpl(conn)` LOB API
    that statements later read from config, both only knowable post-connect. With `Handle` no longer `Configurable`,
    the `handle.configure(...)` sugar it uses stops compiling.
  - **customizeHandle SPI rework — DECISION: OPTION 2 (immutability-consistent, NOT an escape hatch), maintainer-chosen.**
    The per-connection config must be applied DURING handle construction (the mutable-assembly window, like the D6
    `configScope`), never post-construction on a read-only handle. Add a `JdbiPlugin` SPI
    `default void customizeHandleConfig(Connection connection, ConfigRegistry config) throws SQLException {}` (name
    TBD; consider instead a scope-returning `Consumer<ConfigRegistry> handleConfigScope(Connection)` so the plugin
    never holds the registry). Apply it in the `Handle` ctor AFTER the user `configScope` and BEFORE the extension
    context is derived (the ctor has `connection` and can reach `jdbi.getPlugins()`, same package). `Jdbi.open:415`
    `customizeHandle(Handle)` STAYS for non-config customization. **PostgresPlugin:** move `configure(PostgresTypes,
    …)` into the new hook; `VectorEnabler.enable(conn)` (a connection side-effect, not config) → `customizeConnection`
    or keep in `customizeHandle`. **FIRST survey ALL `customizeHandle` impls (main + downstream) for direct
    `handle.getConfig().configure(...)`** — the compiler only flags the `Configurable`-sugar call in PostgresPlugin;
    direct-registry mutations won't have broken the build, so grep for them and move them too. Result: the Postgres
    handle forks its COW child once at construction (correct — genuine per-connection config); every other handle
    stays un-forked, preserving the R3 warm-cache payoff.
  - **Test/doc migration (all → build-time config; NO escape hatch in user/test code):** dominant ~75% are
    `ext.getSharedHandle()` then `handle.registerX/configure(...)` → `ext.withConfig(b -> b.registerX(m))` (the R2
    convenience; class-scoped is fine for the static-fixture majority); `useHandle(h -> { h.configure(...); ... })` /
    `withHandle` → scoped `useHandle(Consumer<ConfigRegistry>, cb)` / `withHandle(scope, cb)` (D6);
    `openHandle()` + configure → `ext.getJdbi().open(scope)`. **Special tail:** `docs` (~24 sites, 4 adoc) are
    user-facing snippets that must compile → migrate to the NEW recommended API (doubles as R6 doc work), don't
    mechanically `withConfig`; kotlin / kotlin-sqlobject (~100 sites, `.kt` — a Java grep misses them, sweep `*.kt`
    separately); methods with CONFLICTING per-method config → `open(scope)` per method, not class-scoped `withConfig`.
  - **Workflow decomposition (hard serialization point at core-test):**
    - **Phase 0 (inline):** the 2 main-source edits + the `customizeHandleConfig` SPI + the PostgresPlugin rework;
      `mvn install -pl core,postgres -DskipTests` to confirm `src/main` compiles.
    - **Phase 1 (fan-out):** core test = 37 files as ~5–6 per-package items (`argument/`, `mapper/`, `collector/`,
      `extension/`, `statement/`, root). **BARRIER**, then install the core test-jar (downstream tests depend on it).
    - **Phase 2 (fan-out):** 1 item per downstream module (~18); verify each with `mvn -pl <m> -am install` (MUST
      `-am` or it links a stale core; MUST be `verify`/`install`, NOT `test-compile`, or checkstyle is skipped — both
      R2 gotchas).
    - **Phase 3 (inline):** full-reactor `mvn clean install` WITH checks, fix stragglers, commit R4.
  - **All-or-nothing:** no green intermediate commit until the whole migration lands (core-test being a downstream
    test-jar dependency forces the serialization). See the TRUE-IMMUTABILITY note below — R4 as scoped already
    delivers problem (1)'s structural fix (the SPI, not an escape hatch); problem (2) is R5.

  > **HANDOFF — TRUE IMMUTABILITY IS THE GOAL (maintainer 2026-07-18).** The end state is a handle whose config is
  > **truly immutable** post-open — no post-construction registry mutation anywhere in idiomatic code. Two structural
  > problems; R4 as scoped above resolves the first, R5 owns the second:
  > **(1) PostgresPlugin `customizeHandle`** binds `PostgresTypes` to the live JDBC connection at handle creation.
  > SOLVED by R4's OPTION 2: the new `customizeHandleConfig(Connection, ConfigRegistry)` SPI expresses "per-connection
  > config computed at open" INSIDE the construction-time assembly window, with no post-open mutation. Do NOT regress
  > this into a post-open `handle.getConfig().configure(...)` escape hatch.
  > **(2) The config interface must not expose mutating methods that silently don't work** — today a read-only
  > surface still hands back a `ConfigRegistry` with a live `configure()`, and immutable config values expose withers
  > (`c.attachAllStatementsForCleanup(true)`) whose return value is easy to discard as a no-op (the recurring
  > discard-mutation foot-gun). The read/mutate split (R5) should make read-only contexts expose only read methods,
  > so a mutating call that can't take effect doesn't compile. Capture the right shape when R5 is designed.
- **R5 — final read/mutate split cleanup + benchmark/verify. [DONE 2026-07-19, whole reactor green — 41 modules,
  4860 tests, static analysis clean.]** Mutation survives only on the builder (assembly) and statements/templates
  (per-execution COW); `Jdbi`/`Handle` are read-only. Closed both TRUE-IMMUTABILITY foot-guns (maintainer chose
  "A + annotate B", with the read-only-delegate refinement):
  - **R5-A (`dc169f3ef`): read-only `ConfigView` delegate.** New `ConfigView extends ConfigReader` (`get` / `readAs` /
    `createChild` / `createCopy` — reads and safe derivations that hand out *fresh* registries); `ConfigRegistry
    extends ConfigView` adds the in-place mutators (`configure`, `install`). `ConfigReader.getConfig()` returns
    `ConfigView`; `Configurable`/`StatementContext` covariantly return `ConfigRegistry`. **`Jdbi.getConfig()` /
    `Handle.getConfig()` return a `ReadOnlyConfigView` wrapper** (forwards reads to the underlying registry but is not
    a `ConfigRegistry`), so neither `getConfig().configure(...)` (won't compile) nor `((ConfigRegistry) getConfig())`
    (ClassCastException) reaches mutation — covariance alone was insufficient (the runtime object was still castable;
    the maintainer flagged this). Internal framework code uses a package-private `configRegistry()` for the live
    registry. The 7 resolver `forRegistry(...)` take `ConfigView`; `readAs` still hands the real `ConfigRegistry` to
    its create-fn (the factory SPIs need it) and is documented `@Alpha`/internal. Only 3 core-test sites broke (moved
    to statement context / scoped `open()` / `createChild()`). **No hot-path regression** (R5-C): classic 4120 /
    template 3512 B/op (R4 was 4208 / 3512).
  - **R5-B (`992df4f67`): `@CheckReturnValue` on immutable-config withers.** A discarded wither
    (`getConfig(SqlStatements.class).timeout(5)` — a silent no-op) now fails spotbugs (`RV_RETURN_VALUE_IGNORED`).
    Verified spotbugs enforces `edu.umd.cs.findbugs.annotations.CheckReturnValue` at method level (NOT class level —
    no `@Target(TYPE)`; errorprone was offered but unneeded). ~90 withers across 27 `JdbiConfig` value classes;
    `createCopy`/getters/statics/void excluded. Legitimate `configure(X, c -> c.wither(...))` returns the value from
    the operator, so it is unaffected. No latent discard bug surfaced.
- **R5-D — DONE (2026-07-19, whole reactor green): factory hygiene closes the `readAs` leak (extended pure Option 3).**
  Maintainer chose (via `AskUserQuestion`) to **extend pure Option 3 through the whole read-only transitive closure**,
  not the Option 9 fallback, after impl surfaced that the closure reaches further than the design's "~115 factory
  impls" estimate. Shipped: `ConfigView.readAs` create-fn narrowed `Function<ConfigRegistry,T>` →
  `Function<ConfigView,T>`; `ConfigRegistry.readAs` hands the create-fn a cached `asReadOnlyView()` (a
  `ReadOnlyConfigView(() -> this)`, package-private for the same-package `TestConfigRegistry`), never `this` — so
  `getConfig().readAs(ConfigRegistry.class, r -> r)` no longer type-checks and `r -> (ConfigRegistry) r` throws CCE.
  The **read/resolve path is now uniformly `ConfigView`; `ConfigRegistry` appears only where mutation is intended**
  (`ConfigCustomizer`, `StatementContext`, `configure`/`install`, `JdbiPlugin.configure(Builder)`/`customizeHandleConfig`,
  the kotlin `Configurer`). Narrowed to `ConfigView` (all verified read-only): the 7 factory SPIs +
  `AbstractArgumentFactory`; `ArgumentFactory.Preparable.prepare` + `QualifiedArgumentFactory(.Preparable)`;
  `RowMapper.init`/`ColumnMapper.init`; the `ExtensionFactory` getter family
  (`getExtensionHandler{Factories,Customizers}`/`getConfigCustomizerFactories`); the `PojoPropertiesFactory` chain
  (`BuilderSpec`/`BuilderPojoProperties`/`BeanPropertiesFactory`/Modifiable); `ConfigCache`/`ConfigCaches`; `Codec`
  (`getColumnMapper`/`getArgumentFunction`); and the `JsonMapper` family (`forType`/`toJson`/`fromJson` +
  jackson2/jackson3/gson2/moshi). All 7 resolvers hold a `ConfigView` and pass it to the narrowed SPIs. **138 files
  across 17 modules** (bigger than the estimate precisely because of `init`/`ExtensionFactory`/`PojoProperties`/
  `ConfigCache`/`Codec`/`JsonMapper`). No mutable-registry factory was found, so the Option 9 fallback was unneeded.
  `TestConfigRegistry` readAs assertions updated to compare against `X.asReadOnlyView()`. **GOTCHAS:** (1) always
  `clean`-compile core — incremental compilation gave a FALSE `BUILD SUCCESS` with ~40 stale factory impls
  un-recompiled; (2) grep for override sites must match `prepare(`/`create(`/`forType`/etc., not just `build(`, and
  must NOT exclude the kotlin module (it lives under `org/jdbi/core/kotlin`, so a `grep -v /core/` wrongly drops it);
  (3) blanket `ConfigRegistry`→`ConfigView` per file over-reaches into non-factory files (`BindingsMixin`,
  `Configurable`) and test bodies (`new ConfigRegistry()`) — narrow only override signatures, or revert and redo
  surgically; (4) ktlint's `function-signature` rule demands the shorter `ConfigView` signature collapse its body onto
  one line, and `ktlint:format` does NOT auto-fix it (manual join + dedent).

- **[superseded design of R5-D — kept for rationale] DESIGNED 2026-07-19,
  maintainer-requested; NOT yet implemented — start a clean session with this.**
  - **The residual leak.** After R5-A, `ConfigView` (what `Jdbi/Handle.getConfig()` returns) still exposes the
    `@Alpha` `readAs(Class<T>, Function<ConfigRegistry, T>)`, whose create-function is handed the real
    `ConfigRegistry`. So `jdbi.getConfig().readAs(ConfigRegistry.class, r -> r)` extracts the mutable registry and
    reopens `configure(...)`. It's obscure and `@Alpha`, but the maintainer wants it closed too ("bulletproof, as
    long as it doesn't lead to awkward contortions"). The naked cast + `getConfig().configure()` are already closed.
  - **Why it's on `ConfigView`.** The `ConfigReader` find-helper defaults (`findMapperFor`, `findColumnMapperFor`,
    `findArgumentFor`, `findCollectorFor`, `findSqlArrayTypeFor`, `findElementTypeFor`, … — **16** overloads that go
    through a resolver) call `XResolver.forRegistry(getConfig())`, and `forRegistry(ConfigView)` calls
    `configView.readAs(Resolver::new)`. The resolver needs the real `ConfigRegistry` to pass to the factory SPIs
    (`ArgumentFactory.build(…, ConfigRegistry)`, `RowMapperFactory.build(…, ConfigRegistry)`, etc.). `readAs` is only
    ever called from the 7 resolvers' `forRegistry` (confirmed) + two internal config-package sites
    (`ConfigRegistry.parent` recursion, `ReadOnlyConfigView` delegate).
  - **RECOMMENDED FIX — Option 9 (minimal, bulletproof for the stated concern, no factory-SPI change).** Take
    `readAs` OFF the public `ConfigView` and keep it on `ConfigRegistry` only (public/`@Internal`, since the resolver
    packages call it cross-package). Then `getConfig().readAs(...)` does not compile, and `((ConfigRegistry)
    getConfig())` is still a CCE — leak closed at compile time. To keep the find-helpers working without `readAs` on
    the view, restructure them so a `ConfigView` resolves via a real `ConfigRegistry` it holds, not via the view:
    - `ConfigView` **re-abstracts** the 16 resolver-backed find-helpers (redeclare the `ConfigReader` defaults as
      abstract on `ConfigView`, forcing implementors to provide a real impl and preventing the registry-leaking
      default from running on a view).
    - `ConfigRegistry` implements each as `XResolver.forRegistry(this).findY(...)` — revert `forRegistry(...)` from
      `ConfigView` back to `ConfigRegistry` (7 resolvers); resolvers keep their `ConfigRegistry` field; SPIs unchanged.
    - `ReadOnlyConfigView` implements each by delegating to `registry.get().findY(...)` (the wrapped real registry's
      helper).
    - `ConfigReader` keeps the 16 helpers as defaults but delegating to `getConfig().findY(...)` (dispatches to the
      concrete `ConfigView` impl) instead of `forRegistry(getConfig())`. **Recursion check:** `ConfigRegistry` and
      `ReadOnlyConfigView` MUST override (they do) or `getConfig().findY()` → `this.findY()` loops; Jdbi/Handle are
      fine (their `getConfig()` returns the wrapper, not themselves). The 3 non-resolver helpers (`getAttributes`,
      `getAttribute`, `getSqlArrayArgumentStrategy` — plain `getConfig(X).getY()`) stay as `ConfigReader` defaults.
    - `createChild()`/`createCopy()` stay on `ConfigView`: they return a **fresh, detached** registry (mutating it
      never affects the source), so they are not a back-channel to *this* config — needed by statements/handles.
    - Fix the ~2 tests that call `forRegistry(handle.getConfig())` (now a `ConfigView`) — `TestPreparedArguments`
      (use the public find/prepare path or a real registry) and `TestConfigRegistry.readAs` (already on a real
      `ConfigRegistry`, so unaffected). Scope ≈ 16×3 impls + 16 default rewrites + 7 forRegistry reverts + ~2 tests
      ≈ 70 small edits, all in the config/statement packages + resolvers. Validate with a full `mvn clean install`.
  - **RECOMMENDED (maintainer leans this way 2026-07-19) — Option 3: narrow the RESOLVING factory SPIs to
    `ConfigView` (factory hygiene). Supersedes Option 9 — cleaner and stronger, and only marginally larger.**
    Rationale (measured): this is NOT a new break — the `org.jdbi.v3`→`org.jdbi` rename already forces every factory
    impl (in-repo + user) to be touched, so a parameter narrowing rides along at near-zero marginal cost; and the SPIs
    cleave cleanly into resolve-only vs configure (the ONLY config-mutators found are `ConfigCustomizer` impls, which
    legitimately configure at assembly time). Plan:
    - **Narrow to `ConfigView`** (resolve-only): `ArgumentFactory`, `AbstractArgumentFactory`, `QualifiedArgumentFactory`,
      `RowMapperFactory`, `ColumnMapperFactory`, `QualifiedColumnMapperFactory`, `SqlArrayTypeFactory` — change each
      `build(…, ConfigRegistry)` → `build(…, ConfigView)`. (`CollectorFactory.build(Type)` already takes no config —
      out of scope.) ~115 impl files (≈70 main + ≈45 test): core 36, postgres 18, vavr 5, kotlin 4, plus 1–2 each in
      json/guava/sqlite/jpa/jodatime2 — a mechanical fan-out like R4. No resolving factory currently mutates config,
      so this is safe; any hidden mutation surfaces as a compile error (the guarantee).
    - **Keep `ConfigRegistry`** (configure hooks — legitimate mutation): `ConfigCustomizer.customize(ConfigRegistry)`,
      `ConfigCustomizerFactory`, `JdbiPlugin.configure(Builder)`, `JdbiPlugin.customizeHandleConfig(Connection,
      ConfigRegistry)`.
    - **`readAs` stays on `ConfigView`** but its create-function receives a read-only view, not the registry:
      `ConfigRegistry.readAs` does `create.apply(this.asReadOnlyView())` (a cached per-registry wrapper). Resolvers'
      `forRegistry(...)` take `ConfigView`, hold a `ConfigView`, and pass it to the now-`ConfigView` factory SPIs. So
      the `readAs` leak is closed as a side effect — `getConfig().readAs(X, c -> (ConfigRegistry) c)` gets a wrapper,
      not the registry. **This makes Option 9's find-helper re-abstraction unnecessary** (the `ConfigReader` defaults
      keep calling `forRegistry(getConfig())`, now `forRegistry(ConfigView)`).
    - **Do NOT change** `StatementContext` (it IS mutable per-statement config), nor the `ExtensionFactory`
      `getExtensionHandlerFactories(ConfigRegistry)` family unless it proves read-only too (check during impl).
    - Fan-out like R4 (core-test barrier, then downstream), full `mvn clean install` gate. Best in a clean session.
  - **Fallback — Option 9 (smaller, if Option 3 proves to have an awkward factory that genuinely needs a mutable
    registry):** the readAs-off-ConfigView + find-helper re-abstraction described above (~70 edits, no factory change).
- **R6 — DONE (2026-07-19): "Upgrading to Jdbi v4" guide drafted below (`## Upgrading to Jdbi v4`), with verified
  before/after benchmarks.** It is written as publication-ready user-facing prose; **lift it into
  `docs/src/adoc/index.adoc` as a top-level `== Upgrading to Jdbi v4` section (and retitle the guide from "Jdbi 3"
  to "Jdbi 4") when v4 is cut.** Kept here for now because the branch is unreleased and the shipped guide still
  documents 3.x. Covers every break: the `org.jdbi.v3`→`org.jdbi` rename; `Jdbi.create(...).registerX/installPlugin/
  setX` → `Jdbi.builder(...)…build()` (`Jdbi` is now read-only); `JdbiPlugin.customizeJdbi` → `configure(Jdbi.Builder)`
  (+`JdbiPlugin.of`) and `customizeHandle`-config → `customizeHandleConfig`; handle-level `handle.registerX/configure`
  → `jdbi.open(scope)`/per-statement; immutable `JdbiConfig` (withers, `@CheckReturnValue`); factory/mapper SPIs take
  `ConfigView`; `getConfig()` is read-only; plus the new `QueryTemplate`.

## Upgrading to Jdbi v4

> **NOTE (design doc):** this section is the R6 deliverable, written in Markdown to read here. When v4 is cut,
> lift it into `docs/src/adoc/index.adoc`, converting the `###` headers to asciidoc `===`/`====`, the fenced
> code blocks to `[source,java]`/`----`, and the tables to `[cols=...]`/`|===`.

Jdbi 4 is a source-incompatible major release. Its theme is an immutable, copy-on-write configuration model:
a `Jdbi` and a `Handle` no longer carry mutable configuration you reconfigure after the fact. You assemble all
configuration once, up front, through a builder; from then on it is read-only and shared. This removes a class
of order-of-initialization bugs, makes a `Jdbi` safe to publish across threads, and lets Jdbi skip most of the
per-statement and per-handle configuration copying that 3.x paid on every call (see Performance, below).

Most applications that only *use* Jdbi (open handles, run statements, attach SQL objects) need only two mechanical
changes: the package rename, and moving configuration into the builder. Applications that *extend* Jdbi with custom
argument factories, mappers, codecs, or config classes have a few more signatures to update.

### The package rename

Every `org.jdbi.v3.*` package is now `org.jdbi.*` (for example `org.jdbi.v3.core.Jdbi` → `org.jdbi.core.Jdbi`,
`org.jdbi.v3.sqlobject` → `org.jdbi.sqlobject`). This is a find-and-replace across imports; no types were renamed.
Because you are editing every file that imports Jdbi anyway, the other source changes below ride along at low cost.

### Assembling a Jdbi: `create(...).registerX(...)` becomes `builder(...)...build()`

`Jdbi` no longer implements `Configurable`; it is read-only. The post-construction mutators are gone:
`installPlugin`, `registerRowMapper`/`registerColumnMapper`/`registerArgument`/…, `configure(...)`, and the knob
setters `setTransactionHandler`/`setStatementBuilderFactory`/`setHandleCallbackDecorator`/`setHandleScope`. Move all
of that into `Jdbi.builder(...)`, which *is* `Configurable`, and finish with `build()`:

```java
// Jdbi 3
Jdbi jdbi = Jdbi.create(dataSource);
jdbi.installPlugin(new SqlObjectPlugin());
jdbi.registerRowMapper(new UserMapper());
jdbi.configure(SqlStatements.class, s -> s.setQueryTimeout(5));

// Jdbi 4
Jdbi jdbi = Jdbi.builder(dataSource)
        .installPlugin(new SqlObjectPlugin())
        .registerRowMapper(new UserMapper())
        .configure(SqlStatements.class, s -> s.queryTimeout(5))
        .build();
```

`Jdbi.create(...)` is retained as the zero-configuration shortcut (`Jdbi.create(ds)` equals
`Jdbi.builder(ds).build()`); reach for it only when you register nothing. `Jdbi.builder(...)` accepts the same
sources as `create(...)`: a `DataSource`, `Connection`, `ConnectionFactory`, or a JDBC URL (optionally with
`Properties` or username/password).

### Config setters became withers, and `configure` takes a `UnaryOperator`

Config classes are immutable. A setter that used to mutate in place now *returns a new value*, and `configure`
takes a `UnaryOperator` that returns the value to install. The `setX(v)` spelling is dropped in favor of a
prefix-free `x(v)` wither (`register(...)` keeps its name). A discarded wither is a no-op and is flagged at build
time by `@CheckReturnValue`, so `getConfig(SqlStatements.class).queryTimeout(5)` on its own will not compile clean:

```java
// Jdbi 3
jdbi.getConfig(SqlStatements.class).setQueryTimeout(5);          // mutated in place

// Jdbi 4
builder.configure(SqlStatements.class, s -> s.queryTimeout(5));  // installs the derived value
```

### Plugins: `customizeJdbi` becomes `configure(Jdbi.Builder)`

`JdbiPlugin.customizeJdbi(Jdbi)` is removed, because a built `Jdbi` can no longer be mutated. Configure the builder
instead by overriding `configure(Jdbi.Builder)`:

```java
// Jdbi 3
public class MyPlugin extends JdbiPlugin.Singleton {
    @Override public void customizeJdbi(Jdbi jdbi) {
        jdbi.registerRowMapper(new UserMapper());
    }
}

// Jdbi 4
public class MyPlugin extends JdbiPlugin.Singleton {
    @Override public void configure(Jdbi.Builder builder) {
        builder.registerRowMapper(new UserMapper());
    }
}
```

For a one-off plugin, `JdbiPlugin.of(builder -> builder.registerRowMapper(new UserMapper()))` is a lambda shorthand.
The connection-time hooks are unchanged in spirit: `customizeConnection(Connection)` and `customizeHandle(Handle)`
remain. Anything that configured a handle's configuration from `customizeHandle` should move to the new
`customizeHandleConfig(Connection, ConfigRegistry)` hook (see the next section).

### Handle configuration: `handle.registerX(...)` is gone

`Handle` no longer implements `Configurable` either; a handle's configuration is a read-only, copy-on-write child
of the `Jdbi`'s. To vary configuration per handle, open the handle with a config scope:

```java
// Jdbi 3
try (Handle h = jdbi.open()) {
    h.registerRowMapper(new UserMapper());
    ...
}

// Jdbi 4 — configure at open
try (Handle h = jdbi.open(config -> config.configure(RowMappers.class, r -> r.register(new UserMapper())))) {
    ...
}
```

`open`, `withHandle`, `useHandle`, `inTransaction`, and `useTransaction` all have an overload that takes a
`Consumer<ConfigRegistry>` config scope. Configuration that should apply to a single statement can still be set on
that statement (statements remain `Configurable`, copy-on-write). A plugin that must configure every handle it sees
(for example to bind driver-specific types on the live `Connection`) implements
`JdbiPlugin.customizeHandleConfig(Connection, ConfigRegistry)`, which runs once as each handle is created.

### Custom factories, mappers, and codecs take `ConfigView`

The read-only half of the split is a new `ConfigView` type: `Jdbi.getConfig()` and `Handle.getConfig()` return a
`ConfigView` (reads and safe derives only), not a mutable `ConfigRegistry`. The resolving SPIs — the ones Jdbi
calls to *build* an argument or mapper — now receive a `ConfigView` instead of a `ConfigRegistry`. If you implement
any of these, change the parameter type (a mechanical edit alongside the package rename):

* `ArgumentFactory.build`, `AbstractArgumentFactory.build`, `QualifiedArgumentFactory.build`, and the
  `Preparable.prepare` variants
* `RowMapperFactory.build`, `ColumnMapperFactory.build`, `QualifiedColumnMapperFactory.build`
* `SqlArrayTypeFactory.build`
* `RowMapper.init` and `ColumnMapper.init`
* `Codec.getColumnMapper` / `getArgumentFunction`, and the `JsonMapper` family (`forType`/`toJson`/`fromJson`)

```java
// Jdbi 3
public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) { ... }

// Jdbi 4
public Optional<RowMapper<?>> build(Type type, ConfigView config) { ... }
```

`ConfigView` exposes everything a factory legitimately needs — `get(SomeConfig.class)`, the `findXFor(...)`
lookups, and `createChild()`/`createCopy()` — but not in-place mutation. A factory has no business reconfiguring
the registry it is resolving against, so this is now a compile-time guarantee rather than a convention. The hooks
that are *meant* to configure — `ConfigCustomizer`, `JdbiPlugin.configure(Jdbi.Builder)`, and
`customizeHandleConfig` — still receive a mutable `ConfigRegistry`.

### New: reusable `QueryTemplate`

Jdbi 4 adds `QueryTemplate`, a handle-independent, reusable query. It renders, parses, and snapshots configuration
once, then executes against any handle without repeating that work — useful for a hot query run many times:

```java
QueryTemplate template = jdbi.buildQueryTemplate("SELECT name FROM users WHERE id = :id");
// reuse across handles/threads:
String name = template.with(handle).bind("id", id).mapTo(String.class).one();
```

### Performance

The immutable, copy-on-write model lets Jdbi share warm configuration instead of copying it. Numbers below are
`gc.alloc.rate.norm` (bytes allocated per operation, a deterministic JMH metric) on H2, JDK 26; "Jdbi 3" columns
are the same benchmarks with the pre-immutability behavior restored (a per-statement/per-handle `createCopy`).

Per-statement on a warm handle (`QueryTemplateBenchmark`):

| Path | Jdbi 3 (createCopy) | Jdbi 4 |
|---|---:|---:|
| Classic `handle.createQuery(...)` | 6104 B/op | 4208 B/op (−31%) |
| Reused `QueryTemplate` | n/a | 3512 B/op (−43% vs classic 3.x) |

One handle per request (`HandlePerOpV3Benchmark`, a fresh handle opened per operation):

| Path | Jdbi 3 (cold copy) | Jdbi 4 |
|---|---:|---:|
| SQL Object attach, `open` | 134580 B/op | 45719 B/op (−66%; ≈1.3x throughput) |
| SQL Object attach, `withHandle` | 135124 B/op | 46043 B/op (−66%) |
| Fluent query, `open` | 13728 B/op | 12260 B/op (−11%) |
| Fluent query, `withHandle` | 14080 B/op | 12650 B/op (−11%) |

The largest win is the "one handle per request" pattern common in web services: a fresh handle now shares the
`Jdbi`'s already-resolved mappers and extension metadata instead of rebuilding them on every open.


## SESSION 2026-07-19b — dev-guide migration DONE + classic-path maintenance analysis

**Developer guide (`docs/src/adoc/index.adoc`) inline-example migration: DONE** (this makes "## What actually
remains" item 2's inline-snippet pass complete). One file, +154/−125; `mvn -pl docs generate-resources` renders
clean (no include/xref/table warnings). API of every rewritten snippet verified against source. Patterns:
- Register-then-query examples → per-statement registration folded into the statement
  (`handle.createQuery(sql).registerRowMapper(...).mapTo(X)`); statements remain `Configurable`.
- Global-registry / self-typed-overload examples + all `jdbi.installPlugin(...)` one-liners → `Jdbi.builder(dataSource)…build()`.
- Callback config (script-semicolons, callback attach-for-cleanup) → scoped `withHandle(configScope, cb)` / `useHandle(configScope, cb)`.
- Configuration + JdbiConfig chapters rewritten for read-only `ConfigView`/`ConfigReader`; module config withers fixed
  (`Jackson2Config.mapper`, `Gson2Config.gson`, `MoshiConfig.moshi`, `TupleMappers.column`); Guice `customize(Jdbi.Builder)`,
  `HandleCallbackDecorator` plugin, template-engine/SqlParser examples migrated. Fixed 3 pre-existing snippet bugs
  (2 missing `)` on `JoinRowMapper.forTypes`, 1 `.`→`,` in the codec builder).
- Version text: heading → "Introduction to Jdbi 4" / "fourth major release"; dependency coordinates → `jdbi-core`/`jdbi-bom`/`jdbi-sqlobject`.
- **Deliberately left** (scope): the module-overview glossary + `jdbi3-*` inside external URLs — a blanket swap is unsafe
  (`jdbi3-guava-cache` is a separate external repo, not renamed; `jdbi3-gson`→`jdbi-gson2` is a name change). Needs
  per-module verification. Tagged `include::` examples were already correct and untouched.

**R2 polish (`Jdbi.builder(ext.getUrl())` → `ext.builder()`): investigated, no change warranted.** Every rebuild
site is intentionally bare — isolation-intent (trackers / init-counting / shared-handle avoidance, each commented),
custom transaction-handler rebuilds (the R4-decided "rebuild from connection source" pattern), or doc-module example
sources. `ext.builder()` / `withConfig(Consumer<Jdbi.Builder>)` remain as public test-support API (currently no in-repo caller).

**Classic query path vs `QueryTemplate` — should the classic path be retired? (maintainer question)**

*Maintenance footprint.* The binding/defining/result surface is ALREADY shared by both paths: `BindingsMixin`
(1706 LOC), `Customizable`/`QueryCustomizerMixin`, `ResultBearing`/`ResultIterable`, `ArgumentBinder`, `ParsedSql`,
render/parse, the `StatementCustomizer` lifecycle, `SqlLoggerUtil`, `StatementBuilder`, `StatementContext`. The only
real duplication is the execute pipeline: `SqlStatement.internalExecute()` (~50 LOC + helpers) vs
`QueryTemplateBinding.executeStatement()` (~57 LOC + `callCustomizers`) — same sequence, same collaborators.
- **Divergence the split already caused:** the classic path emits JFR `JdbiStatementEvent`s (`attachJfrEvent`);
  `QueryTemplateBinding` does NOT. So template queries — including every SQL Object query method (the handler builds a
  `QueryTemplate` per attach) — silently lose JFR statement instrumentation. Concrete argument to unify the engine.
- **Hard constraint:** `QueryTemplate` is query-only. `Update`/`Call` use `internalExecute`; `Batch`/`PreparedBatch`
  have their own batch execute; `Script` too. There is NO template for these, so the classic `SqlStatement`/`internalExecute`
  engine is REQUIRED regardless of what happens to `Query`. Retiring the query path cannot delete the classic engine.

*Benchmark (`QueryTemplateBenchmark`; added `singleUseTemplate`/`singleUseMappedTemplate`; H2, JMH `-prof gc`, single-row SELECT):*

| Path | B/op | ops/ms |
|---|---:|---:|
| classic `handle.createQuery` (one-shot) | 4168 | 1147 |
| reused `template` (warm) | 3512 | 1495 |
| reused `mappedTemplate` (warm) | 3408 | 1587 |
| `singleUseTemplate` (build + execute once) | 6576 | 828 |
| `singleUseMappedTemplate` (build + map + execute once) | 6544 | 846 |

A **single-use template costs +58% allocation and −28% throughput vs classic** (6576 vs 4168 B/op; 828 vs 1147 ops/ms).
Building a reusable template object + config snapshot + binding for a query run once is strictly more work than a
throwaway statement. So the classic one-shot path is the *efficient* path for the dominant "have a handle, run once"
case — a real reason to keep it, not only ergonomics. (Reused template still wins, as designed: 3512 < 4168.)

*Conclusion / recommendation.* The two-path concern is about the ~60-line duplicated ENGINE, not the surface.
1. **Unify the engine (phase-5 tail, still `[ ]` at the "Reimplement Handle.createQuery … on the primitive" item):**
   one execute pipeline shared by `Query`/`Update`/`Call` and `QueryTemplateBinding`. Removes the duplication AND fixes the JFR gap.
2. **Keep the classic `Query`/`createQuery` surface:** after unification it is ~62 LOC of delegation, and
   `SqlStatement`/`BaseStatement` must exist anyway for `Update`/`Call`/`Batch`/`Script`. Marginal maintenance ≈ zero.
3. **Design caveat for phase-5:** do NOT implement `handle.createQuery` as `buildQueryTemplate(sql).with(handle)` — the
   benchmark shows that regresses the one-shot path 4168 → 6576 B/op. The shared primitive must support a one-shot mode
   (no reusable-template allocation, no eager config snapshot).
4. **Does unification preserve the reused-template win (3512 vs 4208)? Yes.** That win is an *input-side* property, not
   an engine property: the template (a) shares one immutable config read-only vs the classic per-statement `createChild()`
   COW fork, and (b) reuses its once-rendered/parsed `ParsedSql` vs classic's per-call re-render/parse (the ~700 B/op gap
   is mostly (b) — see the "remainder = per-call SQL re-render/parse" note). Unification shares only the downstream
   create→customize→bind→execute→resultset sequence, which is identical regardless of who supplies `(config, ParsedSql)`.
   So parameterize the shared core over `(config, ParsedSql, Binding, customizers)`: the template feeds its cached
   snapshot/parse (keeps the win), the one-shot path feeds a COW child + lazy parse (keeps its cost). The template's
   parse-once edge is structural (a one-shot statement has no reuse handle to cache a parse on), so it cannot be erased
   by unification. Confirm empirically when phase-5 lands by re-running all five `QueryTemplateBenchmark` methods: expect
   `classic` ≈ 4200, `template` ≈ 3500, and `singleUseTemplate` NOT leaking into the classic number.

Net: retiring the classic query *surface* saves ~62 LOC at the cost of a large breaking change + a 58%/28% one-shot
regression — a bad trade. Retiring the duplicated *engine* (unify) is the win worth taking.

**Generalize the primitive beyond queries (maintainer direction, 2026-07-19b).** The earlier "QueryTemplate is
query-only" framing undersells it: the shared execution primitive is essentially today's `internalExecute()`, which
Query, Update, AND Call already share; `QueryTemplateBinding.executeStatement()` is a query-specialized *fork* of it.
Fit by statement type (verified against the execute paths):
- **Update — strongest.** `Update.execute(producer)` is `producer.produce(this::internalExecute, ctx)`, identical in
  shape to `Query.execute`; only the terminal `ResultProducer` differs (updateCount/generatedKeys vs results).
- **PreparedBatch — good.** Already parses once and loops `addBatch()` → one `executeBatch()`; reuse win is across
  repeated batch executions; the add-loop is its terminal.
- **Call — moderate.** Runs through `internalExecute()` too; extras are `createCall` (CallableStatement) as the
  creation step and a post-execute out-parameter terminal — both parameterizable (classic already overrides `createStatement`).
- **Batch (plain) and Script — the odd ones out.** Plain `Batch` is multiple ad-hoc SQLs, rendered but not parsed (no
  named params), on a plain `Statement` — nothing to snapshot/bind; `Script` is one-shot statement-splitting DDL. Batch
  is closer to Script than to the parameterized types.

So phase-5 = generalize ONE primitive that returns the *executed statement*, parameterized over (config source:
shared-snapshot vs COW-child; ParsedSql source: cached vs lazy; statement creation: `create`/`createCall`/plain;
terminal: resultset / updateCount / out-params / `executeBatch`). Two ORTHOGONAL decisions fall out:
1. **Generalize the engine primitive to every parameterized type — high value, part of phase-5.** Collapses the
   duplication AND fixes the JFR-instrumentation gap uniformly (route all execution through the one primitive).
2. **Expose reusable *template types* per statement kind (`buildUpdateTemplate`/`buildCallTemplate`/`buildBatchTemplate`)
   — additive, demand-driven.** Update-template first; Call/Batch when a hot loop justifies it.
Caution to document when update-templates land: for BULK update loops, `PreparedBatch` usually beats N reused-update
executions (JDBC batching amortizes the round-trip, which dominates per-exec allocation) — the update-template win is
for repeated *single* updates (per-request upsert), not "insert 10k rows".

## SESSION 2026-07-20 — engine unified + `QueryTemplate` renamed to `StatementTemplate`

The phase-5 engine unification recommended in SESSION 2026-07-19b is DONE, and the reusable-template
type is generalized + renamed. Whole reactor green (41 modules, ~4888 test executions; static analysis
clean on the changed modules core/sqlobject/benchmark; docs render clean). All on `query-templates`.

**Stage A — one execution engine (maintainability win + JFR fix).**
- `QueryTemplateBinding` DELETED (−239 LOC). Its query-specialized fork of the pipeline is gone; the
  reusable template now executes through the SAME `SqlStatement.internalExecute()` as the classic path.
- `StatementTemplate.with(handle)` returns a real `Query` built in "reuse mode": a package-private
  `SqlStatement`/`Query` ctor takes `(ConfigView parentConfig snapshot, cached renderedSql, cached
  ParsedSql)`. `BaseStatement` ctor generalized to `BaseStatement(Handle, ConfigView parentConfig)`; the
  2-arg ctor delegates with `handle.getConfig()`. `StatementContext.config` stays final (child computed
  before ctx creation).
- `parseSql()` carries the ONE input difference (~5 lines): reuse the cached ParsedSql iff
  `cachedParsedSql != null && ctx.getConfig().isUnforked()`; else render+parse. Exact because both the
  fluent `configure` and `StatementContext.define` fork the COW child via `ConfigRegistry.install()`.
- Shared prologue `SqlStatement.prepareStatement(ParsedSql)` (create + addCleanable + customize) extracted
  from `internalExecute`; `PreparedBatch.internalBatchExecute` reuses it and keeps its bespoke
  bind-loop / `executeBatch` / reset. (Did NOT hoist `ctx.setStatement` into prepareStatement — batch
  never called it; behavior kept identical.)
- Template's inlined result-set terminal collapsed to `execute(returningResults()).scanResultSet(...)`
  (identical to `Query.scanResultSet`; `returningResults` already does the EmptyResultSet/NoResults logic).
- JFR now fires for template/SQL-Object queries (previously silently absent) — new `TestStatementJfr`
  proves a template execution emits a `JdbiStatementEvent` (type "Query"). The template also now honors
  `handle.isAttachStatementsForCleanup()` (leak-prevention) and self-closes on failure, and the latent
  cross-thread config-mutation bug (a config-mutating customizer via `ctx.define` on the shared snapshot)
  is fixed by the createChild + isUnforked model.
- **Stage 0 (`ConfigRegistry`):** `configs`/`views` maps are now lazily allocated — an unforked
  `createChild()` is just the registry shell (reads delegate to the parent; `fork()`/full-copy/root
  allocate). Invariant `parent == null <=> maps != null`. Added public `@Alpha isUnforked()`. Makes
  `createChild` ~free and speeds the classic one-shot path too.

**JFR-when-disabled investigation (maintainer asked) — NO fix needed / abandoned.** A clean same-machine
A/B on master (H2 fluent/sqlobject `selectOne`, `-prof gc`) that gated the `JdbiStatementEvent`
allocation recovered ~0 B/op (delta −12/−35, noise): C2 already scalar-replaces the disabled event, as
JFR intends. An earlier "256-268 B/op = JFR" reading was an ARTIFACT of *deleting* the JFR lines (which
shrank `internalExecute` and shifted inlining of unrelated allocations), not the event itself. A
prototyped `JfrSupport` lazy-gate (probe `isEnabled()` + `NoStatementEvent` singleton) on a master
worktree was validated correct but abandoned as pointless; worktree/branch removed. Net: no measured
evidence the unified template regressed — unification is cost-neutral-to-positive and the classic path
is unaffected (slightly better).

**Stage B — `StatementTemplate` + terminal-picks-kind.**
- Renamed `QueryTemplate`→`StatementTemplate`, `MappedQueryTemplate`→`MappedStatementTemplate`,
  `buildQueryTemplate`→`buildStatementTemplate`, `QueryTemplateBenchmark`→`StatementTemplateBenchmark`,
  `TestQueryTemplates`→`TestStatementTemplates` (a single substring rename `QueryTemplate`→
  `StatementTemplate` across 12 files; `git mv` the 4 files; fixed 2 import-order checkstyle nits the
  rename introduced in `Jdbi`/`SqlQueryHandler`).
- `StatementTemplate.with(handle)` returns a `Query` that now also carries the update terminals
  (`execute()`→int, `executeLarge()`→long, `executeAndReturnGeneratedKeys(String...)`), so which terminal
  you call picks query vs update — the "one template, terminal picks the kind" shape.
- `Script`/plain `Batch` stay out of scope (no named params / one-shot DDL).
- Docs (`index.adoc`): prose generalized query→statement + an update-via-template example added.

**Tier 1 — reusable Call/PreparedBatch template accessors — DONE (same session, committed).** The
maintainer directed "v4 finishes the work, not minimal." `Call` and `PreparedBatch` each got the same
package-private reuse-mode ctor `(Handle, ConfigView, CharSequence, String renderedSql, ParsedSql)` that
`Query` has, and `StatementTemplate` gained `call(handle)` → `Call` and `prepareBatch(handle)` →
`PreparedBatch` (alongside `with(handle)` → `Query` for query/update). So one template materializes as any
parameterized kind. Update needs nothing extra — `with(handle).…execute()` already runs an update.
Tests: `TestStatementTemplates.testPreparedBatchViaTemplate` (H2), `TestCallable.testCallViaTemplate`
(PG stored proc, reusable). `index.adoc` documents `call()`/`prepareBatch()`.

**Tier 2 — SQL Object adopts templates for `@SqlUpdate`/`@SqlCall`/`@SqlBatch` — DONE (2026-07-20, whole
core+sqlobject+benchmark reactor green with checks + tests).** `@SqlQuery` had been the only handler that
built a `StatementTemplate` per attach; now all four parameterized SQL Object statement kinds do, so each
per-request invocation skips render + parse + config-snapshot. Each handler mirrors `SqlQueryHandler`:
memoize a `StatementTemplate` per attach, bake the CONFIGURE-phase customizers into its snapshot once, and
per invocation bind a fresh thread-confined statement applying only BIND-phase customizers; a method with a
`ConfigMutating` (LATE) customizer falls back to the classic per-invocation path.
  - **`@SqlUpdate`** (`SqlUpdateHandler`): `statementFactory` override builds the template; per invocation
    `template.with(handle)` returns a `Query` and the update returners run through its `execute()` /
    `executeLarge()` / `executeAndReturnGeneratedKeys(...)`. **Both** paths (fast and classic-fallback) now
    yield a `Query` — `createStatement` returns `handle.createQuery(locatedSql)` — so the single
    `resultTransformer`/`configureReturner` is uniform and the `Update` type is no longer referenced by the
    handler. **Observability change (accepted):** `@SqlUpdate` now reports statement type `"Query"` (was
    `"Update"`) in the JFR `JdbiStatementEvent.type` and `StatementContext.describeJdbiStatementType()`.
    That property is `@Beta`, this is the unreleased 4.0.0 major, and it is the direct consequence of the
    Tier 1 "terminal picks the kind" model ("Update needs nothing extra"). No test asserted the old
    `"Update"` type. (The other test breakages the adoption surfaced were real bugs — see the phase-model
    refinement and cache-fix bullets below.)
  - **`@SqlCall`** (`SqlCallHandler`): same pattern via `template.call(handle)` → `Call`; returner is
    `Call.invoke()`. Both paths yield a `Call` (classic-fallback keeps `handle.createCall`).
  - **`@SqlBatch`** (`SqlBatchHandler`): it fully overrides `attachTo` for chunked execution, so the
    template is a memoized `Supplier<StatementTemplate>` built in `attachTo` (null when LATE). Each chunk's
    `PreparedBatch` comes from `template.prepareBatch(handle)` (helper `newPreparedBatch`) and per-row
    customizers apply BIND-only on the fast path (all phases on the classic path); the chunking loop and
    `transactional` wrapping are unchanged.

**Phase model refinement — the right axis is what a customizer touches, not where the annotation sits.**
Making all four kinds share the template engine surfaced two cases the old CONFIGURE/BIND/LATE model got
wrong, both fixed in the direction of unification (maintainer: "phases are not set in stone; find the right
model"):
  - **`@OutParameter` (and `@OutParameterList`)** is a *method-level* customizer that operates on the live
    `Call` (`((Call) stmt).registerOutParameter(...)`). The old model classified type/method customizers as
    CONFIGURE and applied them to the build-time `ConfigureStatement` surface → `ClassCastException`. Root
    cause: annotation *position* (type/method vs parameter) is the wrong proxy for *what the customizer
    touches* (configuration vs the live statement). Fix: a new public marker
    `org.jdbi.sqlobject.customizer.StatementScoped` (symmetric with `ConfigMutating`) declares a customizer
    that acts on the executable statement and must run per invocation → `phaseFor` returns `BIND` for it even
    at type/method level. `OutParameterFactory`/`OutParameterListFactory` declare it. `ConfigMutating` still
    wins → `LATE`. This is invariant-but-statement-scoped work: applying it per invocation is correct (it is
    idempotent), it just cannot be baked into a shared config snapshot.
  - **Issue #1516** (`TestSqlBatchWithCustomizer`): a method-level customizer that does
    `stmt.addCustomizer(...)` is CONFIGURE (bakeable) and is now baked once, so its `beforeExecution` fires
    once per batch execution with a non-null statement. The old classic path re-applied it per row, so the
    test asserted `count == rowCount` (3). #1516's actual bug was a *null statement* (NPE) for `@SqlBatch`,
    not per-element invocation — so the unified behavior (registered once, fires once, non-null stmt) fully
    satisfies the issue. The test was updated to assert `count == 1` and to assert the statement is non-null
    (the real regression guard).

**Latent `DefaultJdbiCache` exception-safety bug — fixed (required by the template pre-parse).** A throwing
cache loader left the placeholder node in the CHM and already added to the expunge queue, so the next `get`
for the same key crashed with `IllegalStateException: Can not add node twice!` instead of retrying. This is
pre-existing (confirmed by a unit probe and by reasoning: calling any parse-failing SQL twice on the classic
path already crashed on the second call). The `StatementTemplate` constructor pre-parses at build time and
swallows a parse failure (SQL may depend on per-execution attributes), so a genuinely-invalid statement
(`TestPositionalBinder`'s mixed named/positional params) parsed twice — once at build, once at execution —
surfaced the bug on the first call. Fix in `DefaultJdbiCache.doGet`: compute the value *before* `addHead`, and
on loader failure `cache.remove(key, node)` so a later call retries with a fresh node. Regression test
`DefaultJdbiCacheTest.testThrowingLoaderDoesNotCorruptCache` (throw twice, then a successful load).
  - **Tests:** `TestSqlObjectTemplateAdoption` (H2, 4 tests) — a "configure applied once per attach" counter
    proves template reuse for update + batch, and `ConfigMutating` (reusing `TestConfigMutatingCustomizer.
    DefineViaConfig`) proves the classic fallback for update + batch. `TestSqlCall` gained
    `sqlCallReusesTemplateAcrossInvocations` (the old `testFoo` never actually invoked the `@SqlCall`
    handler). Previously-failing suites now pass on the unified path: `TestOutParameterAnnotation` (15),
    `TestPostgresRefcursorProc`, `TestPositionalBinder` (5), `TestSqlBatchWithCustomizer` (1). All existing
    SQL Object suites exercise the fast path (it is now the default) and stay green.
  - **A/B benchmark (R7 method: revert only the 3 handler files to HEAD, rebuild, rerun, restore; H2, JDK
    26, `-prof gc`, 2 forks × 4 iters; `gc.alloc.rate.norm`, OUT→IN):** `sqlobjectInsertRowCountBindBean`
    7770→7408 B/op (**−361, −4.6%, +8% thrpt**), `sqlobjectInsertGeneratedKeyBindBean` 8500→8246 (**−254,
    −3.0%**), `sqlobjectInsertGeneratedKeyValues` 8724→8557 (**−167, −1.9%**), `sqlobjectInsertRowCountValues`
    7705→7679 (−26, noise). `sqlobjectInsertBatch` (10 rows) 52561→52501 (−60, ~0% — no regression; the
    one-time render/parse/snapshot saving amortizes across the rows, matching the 19b prediction that bulk
    batch does not benefit but must not regress). Controls flat: `fluentSelectOne` 4121→4121 (byte-identical),
    `sqlobjectSelectOne` 4592→4581. New benchmark `BaseSqlObjectV3Benchmark.sqlobjectInsertBatch` (+ DAO
    `insertTestDataBatch`) added for the batch A/B. Net: modest-but-real per-op update win, no batch
    regression, and all four kinds now share one template engine (uniform JFR + maintainability).

**Follow-ons noted:** (1) the abandoned JFR lazy-gate is genuinely not worth doing (event is
scalar-replaced when disabled); (2) `## Upgrading to Jdbi v4` and the `## SESSION 2026-07-19b` benchmark
tables still say `QueryTemplate` / `QueryTemplateBenchmark` — sweep those names to `StatementTemplate`
when the guide is lifted into `index.adoc` at release; also note the `@SqlUpdate` `"Update"`→`"Query"`
statement-type change in the upgrade guide's observability/QueryTemplate area at release.

## What actually remains (2026-07-19)

The design and implementation are complete: the `QueryTemplate` feature, the immutable config model (phase-2
through D7/sub-step-5), the assembly-API redesign (D1–D6), and the whole REMOVAL phase (R1–R7 + R5-D) are all
committed and the whole reactor is green with tests and static analysis. The `## Config assembly API`, `## D4b
proposal`, `## Config/Handle decoupling`, and `## Tasklist` sections below are **historical design records** kept
for rationale — they are accurate as history, not as an open to-do list. Only release-time work is left:

1. **Ship it.** This branch IS jdbi 4.0.0-SNAPSHOT. The remaining step is cutting the release (version, release
   notes, the `org.jdbi.v3`→`org.jdbi` rename is already in). No further code changes are required for the feature.
2. **Finish the asciidoc migration. [inline-snippet pass DONE 2026-07-19b — see the SESSION 2026-07-19b section above.]**
   The `docs/src/adoc/index.adoc` guide is retitled, carries `== Upgrading to Jdbi 4`, and the ~50 hand-authored
   `[source,java]` snippets that used the removed mutable API were migrated (builder / per-statement / config-scope
   per snippet); the `== Introduction to Jdbi 3` heading + "third major release" prose and the `jdbi3-core`/`jdbi3-bom`
   dependency coordinates were fixed to Jdbi 4 / `jdbi-*`. **Only leftover (deliberately deferred):** the module-overview
   glossary `jdbi3-*` names and `jdbi3-*` inside external URLs — a blanket swap is unsafe (`jdbi3-guava-cache` is a
   separate external repo, not renamed; `jdbi3-gson`→`jdbi-gson2` is a name change), so it needs a per-module
   verification pass against the module repos. Tagged `include::` examples were already correct.
3. **Merge jdbi/jdbi#2992 by mostly removing it** (share the `ExtensionMetadata` cache): the immutable world +
   handle-boundary copy-on-write already deliver its payoff (R3/R7), so when #2992 lands on master the merge is
   largely a deletion. See the memory note `pr-2992-merge-mostly-remove`.
4. **Delete this design doc** once the feature ships (it is a working document, not part of the shipped tree).

### Pre-bound result mappers on a template — DONE (2026-07-19)

Implemented and shipped in this branch. `QueryTemplate.mapTo(Class/GenericType/QualifiedType/Type)` resolves the
`RowMapper<T>` once, at build time, against the template's fixed config snapshot (`MapperResolver.forRegistry(config)`
+ `findMapper`), and returns a `MappedQueryTemplate<T>`. `MappedQueryTemplate.with(handle)` returns a
`BoundMappedQuery<T>` that reuses `QueryTemplateBinding` for all binding/defining/customizing (it implements
`QueryCustomizerMixin<BoundMappedQuery<T>>` and delegates the ~7 abstract accessors to the wrapped binding, so it
inherits the whole `BindingsMixin` surface for free) and adds one terminal, `results()`, returning `ResultIterable<T>`
via `binding.map(preResolvedMapper)` — the mapped analogue of `.mapTo(X)`, so the execution chain is the same length
(`mt.with(h).bind(..).results().one()`). Resolution is eager, so an unmapped type raises `NoSuchMapperException` at
build time instead of first execution. The design stayed additive: the non-generic `QueryTemplate` + per-execution
`ResultBearing` path (`reduceRows`/`collectInto`/`stream`) is untouched. Deliberately did **not** add
`jdbi.buildQueryTemplate(sql, X)` convenience overloads — `jdbi.buildQueryTemplate(sql).mapTo(X)` is already fluent,
and one way is better than two.

Verified win (JMH `-prof gc`, H2, `QueryTemplateBenchmark`, single-column `String` result — the `SingleColumnMapper`
wrapper path that benefits most): `mappedTemplate` 3408 B/op & 1603 ops/ms vs `template` 3512 B/op & 1509 ops/ms =
**−104 B/op (−3.0% allocation), +6.2% throughput**; vs `classic` 4208 B/op & 1158 ops/ms = −19% allocation, +38%
throughput. The mapped number is identical to a hand-held pre-resolved mapper (measured), so the API adds zero
overhead over the theoretical ceiling. Matches the pre-implementation estimate (~100–300 B/op, low end; small
throughput bump, slightly beaten). Modest but real; a genuine opt-in refinement for a hot, single-shape query.

### Future: fold the `warm` path into an eager template build — candidate cleanup (not scheduled)

`ExtensionHandler.warm(config)` (implemented by `CustomizingStatementHandler`/`ResultReturner`/customizers) is a
bolt-on: at attach time `ExtensionMetadata.ExtensionHandlerInvoker` calls `extensionInvoker.warm(methodConfig)`,
which eagerly resolves the result mapper (`ResultReturner.warm` → `config.findMapperFor(type)`) and argument
factories; `Extensions.isFailFast()` only gates whether a resolution failure rethrows (the resolution runs
either way). It runs once per attach (the invoker is created once per attach in `ExtensionFactoryDelegate` and
cached), and in practice only the SQL Object statement handlers implement it. It does two jobs: **fail-fast
validation** (surface an unresolvable mapper at wiring time) and **cache pre-warming** (pay resolution before the
first call).

Both jobs are a natural fit for an **eager, mapped template built at attach**. Today the SQL Object handlers build
the template *lazily* (first call) via `MemoizingSupplier` and resolve the result mapper *eagerly* through the
separate `warm` traversal — an inconsistent split. If instead each handler built a `MappedStatementTemplate` at
attach (`StatementTemplate.mapTo(elementType)` already resolves the mapper up front and throws
`NoSuchMapperException` at build), the template build would *be* the warm step: render + parse + config snapshot +
mapper resolution, all once, in the place that already owns "prepare up front." Fail-fast falls out (gate the
build's `NoSuchMapperException` on `isFailFast()`); pre-warming falls out (everything resolved into the shared
snapshot). The parallel `warm()` traversal could then be dropped on the template path.

Caveats that keep it from being a trivial deletion: (1) `warm` also pre-resolves `@Bind` parameter customizers by
static parameter type — to fully match, the eager build would need to prime argument resolution too (the types are
static, so this is doable, but it is the one piece the mapped template does not subsume for free); (2) the
`ConfigMutating`/LATE classic-fallback path builds no template, so it would still rely on `warm` (or accept
first-call errors); (3) `warm` is a general `ExtensionHandler` SPI (default no-op) — since only statement handlers
use it, the hook can stay while the template path stops needing it, and retiring the SPI is a larger
extension-framework change. Net: `warm` is **not inherently necessary** — it predates the template model and the
model can absorb it; this is a good post-release phase-6 simplification, not blocking.

## Historical design records (below)

The remaining sections document how the end state was reached. They are retained for rationale and were accurate
when written; consult the memory `query-templates-branch` and git history for the authoritative committed state.

### Phase-2 immutable-config history

`configure` is `UnaryOperator`-based; config *values* were made immutable domain-by-domain (D1). The sub-step-3
curated set — `Enums`, five scalar-policy configs, `Arguments` (with a bulk `register(Collection)`), the interceptor
trio (`RowMappers`/`ColumnMappers`/`SqlArrayTypes`, with `withInferenceInterceptor` + a shared `RegistrationLists`
helper), the shared `MapEntryConfig` (`MapEntryMappers` + vavr `TupleMappers`, whose registry back-ref was removed),
`Extensions`, and `SqlStatements` — plus sub-step 4 (below) converted the rest.

**sub-step 4 — DONE (2026-07-18): every `JdbiConfig` value is now immutable and `setRegistry` is deleted.** An
earlier note wrongly claimed "SqlStatements is the last D1 config"; `createCopy()` across all ~34 `JdbiConfig`
impls showed ~19 still mutable. All are now converted (each its own whole-reactor-green commit):
- `EnumStrategies` → deleted; became the per-registry `EnumStrategyResolver` (`readAs`), removing a real
  resolution back-ref (it depended on the registry's `Enums`/`Qualifiers`).
- `JdbiCollectors`, `PojoTypes` → immutable (register returns new; resolvers already existed).
- `Qualifiers` → immutable; the `setRegistry` back-ref became a `final` registry via a `(ConfigRegistry)`
  constructor (used only to reach the registry-family's shared, registry-independent qualifier caches).
- Module data configs → immutable: `JsonConfig`, `Gson2Config`, `MoshiConfig`, `Jackson2Config`,
  `Jackson3Config`, `FreemarkerConfig`, `StringTemplates`.
- Behavior/registration configs → immutable: `Handles`, `OnDemandExtensions`, `SqlObjects`, `Handlers`,
  `HandlerDecorators`, `SerializableTransactionRunner.Configuration`.

**The two registration facades — DONE (2026-07-18, decision: option "clean, drop setRegistry").** `JdbiImmutables`
and `PostgresTypes` were registration *facades* whose `register*` methods wrote into ANOTHER config
(`PojoTypes` / `SqlArrayTypes`) via a per-registry `setRegistry` back-ref — a self-returning immutable wither
cannot reach a sibling config, which is why they lagged. Resolved by removing the sibling write, not the ref:
- `JdbiImmutables` (`fb4e06797`): held no state of its own, so it is **deleted**. `registerImmutable`/
  `registerModifiable` (all 8 overloads) move to `Configurable` as conveniences over `PojoTypes` (exactly
  parallel to `Configurable.registerArrayType` over `SqlArrayTypes`); the Immutables reflection moves verbatim
  into the internal `ImmutablesFactory`. Public break: `getConfig(JdbiImmutables.class).registerImmutable(X)` →
  `jdbi.registerImmutable(X)`; `.withConfig(JdbiImmutables.class, ...)` sites → a `customizeJdbi` plugin.
- `PostgresTypes` (`8c810af8c`): the eager `SqlArrayTypes` fan-out becomes a single data-driven
  `PostgresCustomTypeArrayFactory`, registered once by `PostgresPlugin`, that resolves a custom type's array
  type name by consulting `PostgresTypes` at bind time. `PostgresTypes` is then a plain immutable value —
  `types` immutable, `registerCustomType` a wither, `lob` final (so the statement-path `getLobApi` read is safe
  on a shared value), `setLobApi` → `lobApi` wither, `createCopy` returns `this`, no back-ref.
- **`setRegistry` deleted from the `JdbiConfig` SPI** (`b710cb60b`): with both facades' refs gone, no config
  needs post-construction registry injection (Qualifiers etc. take it via a `(ConfigRegistry)` ctor). Removed
  the method and its three `ConfigRegistry` call sites (copy-ctor re-wire, `install`, on-demand `configFactory`).
  Every config value's `createCopy` now returns `this`, so the registry copy-ctor shares all values by reference.

**D7 + sub-step 5 — DONE (2026-07-18, `ebc1e3b2a`): copy-on-write statement config, the perf payoff.**
Added `ConfigRegistry.createChild()` — a copy-on-write child that holds no config until its first write: reads
and memoized resolver views delegate to the parent (so an unmodified statement reuses the handle's warm
resolvers), and the first `configure()` forks a private snapshot and detaches, leaving the parent untouched.
`install()` now clears the memoized views so a reconfigured registry never serves a resolver built against a
superseded value. `BaseStatement` uses `createChild()` instead of `createCopy()`; `createCopy()` stays for the
isolated-snapshot callers (`buildQueryTemplate`, the `Handle` default config, `ExtensionMetadata`). Benchmark
(`QueryTemplateBenchmark.classic`, gc profiler, same build): `createCopy` 6104 B/op @ 806 ops/ms → `createChild`
4208 B/op @ 1174 ops/ms (~31% less allocation, ~1.46× throughput); classic now approaches the reused-template
path (3512 B/op). Behavior change consistent with the "init once per `ConfigRegistry`" contract: two statements
on one handle that change no config share the handle's registry, so a mapper's `init()` runs once and is reused
(re-inits only when a statement changes config and forks); clarified the `ColumnMapper`/`RowMapper.init` javadoc
and updated `TestMapperInit`.

Remaining: **D4/D5/D6** (the builder + plugin SPI + deprecations + open-scope) — the largest public surface.
All commits are whole-reactor green with static analysis ENABLED (validated via full `mvn clean install`) — do
not fall back to `-Dbasepom.check.skip-all` as the validation of record.

**Build gotcha (2026-07-18):** a partial `mvn install -pl a,b,c` WITHOUT `-am` links against stale local-repo
artifacts of unlisted dependency modules — this surfaced as a runtime `NoSuchMethodError` for the
already-removed `RowMappers.getInferenceInterceptors()` from a stale `jdbi-kotlin` jar. Use `-am`, or validate
with a full `mvn clean install`.

**Done earlier (sub-steps 1–2, committed, whole reactor green):**
- Sub-step 1 (`03017fe`): `ConfigRegistry.readAs(asType, factory)` — the per-registry memoized-view seam that
  resolvers are built on (a fork starts with an empty view cache, so it never serves a stale resolution).
- Sub-step 2: resolution + caches (+ registry back-refs used solely for resolution) moved off the config
  values onto per-registry resolvers via `readAs`, so each value is registration data (+ policy) only:
  `MapperResolver` (row/column; `Mappers` facade deleted), `ArgumentResolver`, `CollectorResolver`,
  `ArrayTypeResolver`, `PojoResolver`, `ExtensionMetadataResolver`. This session's `EnumStrategyResolver`
  finished the pattern for `EnumStrategies`.

### Design notes for D7 / sub-step 5 (statement zero-copy) — IMPLEMENTED (`ebc1e3b2a`); kept as rationale
The perf payoff (delete the per-statement `createCopy()` at `BaseStatement.java:36`) needs a lazy
copy-on-write statement registry. Key findings so they are not re-derived:
- **`ConfigRegistry.install` is the single write choke point.** All mutation routes through
  `configure(Class, UnaryOperator)` → `install`; values are immutable, so nothing mutates a config value in
  place. A copy-on-write registry only has to intercept `install`.
- **`createCopy()` is used two ways** — do not blanket-convert it: as a *snapshot that must stay isolated*
  (`Jdbi.buildQueryTemplate`, the `Handle` default config, `ExtensionMetadata` per-method/instance config),
  and as an *ephemeral child* (`BaseStatement`). Only the ephemeral-child use should become lazy; add a
  separate `createChild()` rather than changing `createCopy`.
- **A correct lazy child requires every statement-reachable config to be immutable & shareable**
  (`createCopy()==this`, no per-registry back-ref). That is why the sub-step-4 config-value immutability work
  was the prerequisite. **This precondition now holds:** sub-step 4 is complete, every config value's
  `createCopy` returns `this`, and `setRegistry` is deleted from the SPI — no config carries a back-ref.
- **Sketch of the child** (parent-delegation model): the child holds only its own overrides and delegates
  reads to the parent; the first `install` materialises (or the child forks) so writes never touch the
  parent. Watch two hazards: on-demand config creation must not mutate the shared parent map, and a value
  carrying a back-ref cannot be re-pointed on a shared instance. Both are now moot — all values are immutable.

### Warm ExtensionMetadata across handles (jdbi/jdbi#2992) — open, to address in D4/D6
Upstream v3 PR #2992 fixes a real production bug: `ExtensionMetadata` (the reflected SQL Object wiring) was
cached per config copy, so a service that only does `handle.attach()` (one handle per request) recomputed all
metadata every request — the cache stayed permanently cold. Its v3 fix shares a mutable cache map by reference
across copies and adds manual `invalidateMetadataCache()` (called from every `register*`), a `ConfigRegistry`
back-ref on `Handlers`/`HandlerDecorators`, and `volatile`/`AtomicBoolean` publication. That bundle is exactly
the mutable-registry-keyed-cache-plus-manual-invalidation coupling this project removes; **do not port it.**
Note: #2992 is **not** in the jdbi4 base and its machinery is absent here — but the underlying problem is still
present, because our `ExtensionMetadataResolver` lives in the registry's `views` (per-registry, not copied), and
a handle is a `createCopy()` of the Jdbi config, so each handle's metadata cache is cold. Two clean fixes the
immutable world gives us, neither needing #2992's plumbing:
- **COW at the handle boundary.** Have a non-reconfigured handle be a `createChild()` of the Jdbi config (like a
  statement is of the handle), so handles share the Jdbi registry's warm resolvers. Safe only once the Jdbi
  config is frozen after first open (**D4**) — an un-forked child is a live view of a mutating parent, and
  handles run on different threads. So this is a D4 deliverable, and a reason to freeze-on-open.
- **Value-keyed caching.** Key metadata on the immutable config value's identity: a config change is a new value
  → automatic miss, no manual invalidation. Needs config addressable as one immutable value (the D-series end
  state), because metadata can depend on arbitrary registry config via `extensionFactory.get…(registry)`.
When #2992 lands upstream and master is merged in, take it **mostly as a removal**: keep its intent/regression
tests, drop its shared-mutable-cache + `invalidateMetadataCache` + back-ref + atomics — our COW + `views.clear()`
already covers correctness. (The `customizerCount` staleness guard in `ExtensionMetadataResolver`, which the D7
`install()`→`views.clear()` subsumed, was removed in this pass.)

---

## Config/Handle decoupling (2026-07-17): the immutable-config end state — supersedes the sub-step-3 swap sketch

**Why the swap was rejected.** Making `ConfigRegistry` immutable and having `Jdbi.configure`/`Handle.configure`
*fork a new registry and swap a `volatile` reference* achieves immutable config *values*, but the stateful
holders still carry reconfigurable state — the identity of "the current config" changes over its lifetime.
That is the very thing this project set out to remove. A prototype of the swap (volatile `Jdbi.config`,
`Handle` reworking its `final ExtensionContext`, a `configLock`) was written and reverted; the only piece of it
that has a future is as a *deprecated compatibility shim* (see migration), not as the model.

**The target ownership split.** Separate the two concerns that `Handle` currently fuses:

| Concern | Owner | Lifetime / mutability |
| --- | --- | --- |
| JDBC `Connection`, transaction state, cleanables, `StatementBuilder`, handle listeners | `Handle` | mutable, thread-confined, scoped to the handle |
| Config: mappers, arguments, array/collector/pojo/extension registration, defines-defaults, policy | an **immutable `ConfigRegistry` value** | immutable; a changed config is a *new value*, never a mutation |
| Root config assembly | a **`Jdbi` builder / configuring factory** | mutable only during build; frozen at `build()` |
| Per-statement / per-template config additions (`query.registerRowMapper`, `define`, per-exec customizers) | the statement/template **build step** | accumulates locally, frozen into one immutable snapshot at execute |

**Config as a value.**
- `ConfigRegistry` becomes immutable over a persistent map of immutable `JdbiConfig` values. `get(Class)` reads;
  deriving a change is `config.with(Class, op)` → a *new* registry that structurally shares the unchanged
  values (cheap). No `setRegistry` back-ref remains anywhere (sub-step 2 already removed the resolution ones;
  `Arguments`' registration-time ref becomes a build-time concern).
- `JdbiConfig` values are immutable (sub-step 4): `register(...)` returns a new value; policy configs are records.

**Handle.**
- `Handle` holds a *reference* to an immutable config, captured at `open()` from the `Jdbi`. It never mutates
  it. `handle.getConfig()` returns the immutable value.
- Per-handle customization (the legitimate, less-common case) is expressed as deriving a scoped config at open:
  `jdbi.open(cfg -> cfg.with(...))` (exact shape TBD), producing an immutable config the handle references.
- Extension-method scoping already derives an immutable config per call (`ExtensionContext`); that pattern is
  the general mechanism and stays — it is *derive-and-reference*, not mutate.

**Statements / `QueryTemplate`.**
- A statement/template is built from the handle's immutable config plus any local additions accumulated during
  fluent building, frozen into one immutable snapshot at execution. This is exactly today's `QueryTemplate`
  model; classic `Query`/`Update` are reimplemented on the primitive (the remaining phase-5 tail).
- The per-statement `createCopy()` at `BaseStatement.java:36` disappears (sub-step 5 = the perf payoff): no
  local additions → the statement shares the handle's immutable config; additions → one derived immutable
  snapshot. Either way it is a single snapshot with no defensive copy.

**`Configurable` fate.** Split reading from mutating. `getConfig()` stays widely. The mutating surface
(`configure`/`registerX`) is removed from the long-lived `Jdbi`/`Handle` and survives only where scoped-then-
frozen mutation is ergonomic: the `Jdbi` builder (assembly) and the statement/template build step
(`query.registerRowMapper(...).mapTo(X)...`). Fluent per-statement config is legitimate and stays.

**Compatibility — this is the expensive part, and it drives the staging.** Removing post-construction
`jdbi.registerX`, `handle.registerX`, and in-place plugin mutation (`customizeJdbi(Jdbi)`) is a large public
break (thousands of call sites in the wild, ~93 in this repo). Staged migration:
1. **[useful now, no break]** Immutable config *values* (sub-step 4) + immutable `ConfigRegistry` with `with()`.
   The foundation of everything below; not throwaway under any decision.
2. Add the `Jdbi` builder / configuring factory that assembles config and freezes at `build()`. New idiomatic path.
3. Keep legacy `Jdbi.create()` + `registerX` and `handle.registerX` as **deprecated shims** backed by
   *derive-and-re-reference* (this is where the reverted swap returns — as a compat shim, not the model), so
   existing code keeps working while the builder path is preferred. Plugins: `customizeJdbi(Jdbi)` →
   a config-contributing SPI, old one deprecated.
4. Delete the per-statement `createCopy()` (sub-step 5, the perf payoff) — safe once config is never mutated
   after a statement captures it.
5. Long-term: remove the deprecated shims.

**Recommendation / what to do next.** Do the foundation now — it is required under either model and is not
throwaway: (a) make the remaining `JdbiConfig` values immutable (`register` returns a new value; policy configs
become records) — sub-step 4; (b) make `ConfigRegistry` immutable with a `with(Class, op)` derivation. Only
after that is green do we design the *public* builder/open-time API (step 2–3 above), which is a genuine
public-API redesign and wants maintainer sign-off on the exact shapes (`Jdbi.builder()`, `jdbi.open(scope)`,
the plugin SPI). Do **not** re-add holder swap as the primary model.

---

## Config assembly API — PROPOSAL for sign-off (2026-07-17)

Concrete public shapes for the config/Handle decoupling. Grounded in the current surfaces: config funnels
through `Configurable.configure(Class<C>, Consumer<C>)` (~30 convenience methods delegate to it); `Jdbi` also
carries non-config knobs (`installPlugin`, `setTransactionHandler`, `setStatementBuilderFactory`,
`setHandleCallbackDecorator`); `JdbiConfig` today is `{ createCopy(); setRegistry(); }`; `JdbiPlugin` has
`customizeJdbi(Jdbi)`, `customizeHandle(Handle)`, `customizeConnection(Connection)`.

**Three configuration scopes, and where "mutation" is allowed to live:**
1. **Jdbi (root)** — assembled at build time, then frozen. Immutable.
2. **Handle** — config chosen at `open` (optionally derived from the Jdbi's), then fixed for the handle's life;
   the Handle owns only connection + transaction + cleanables.
3. **Statement / template** — fluent per-statement additions on a private copy-on-write registry, frozen at
   execute. This scope legitimately keeps a mutate-then-freeze surface (short-lived, thread-confined).

### D1 — `JdbiConfig` value contract → immutable
- Config values become immutable. Mutators become withers returning a *new* instance:
  `RowMappers.withMapper(m)`, `SqlStatements.withTemplateEngine(e)`; policy configs become records with `withX`.
- Drop `createCopy()` (nothing deep-copies an immutable value) and `setRegistry()` (no back-ref; sub-step 2
  already moved resolution off the values). `JdbiConfig<This>` becomes a minimal marker.
- **Status (2026-07-18):** all values are immutable and `setRegistry()` is **deleted** from the SPI.
  `createCopy()` is retained transitionally — every impl now returns `this`, but the method stays until
  sub-step 5 reworks the two callers (isolated snapshot vs ephemeral child; see the D7/sub-step-5 notes).
- **Break:** every custom `JdbiConfig` implementation. Justified for v4; the SPI is small.

### D2 — `configure` → `UnaryOperator` [SIGNED OFF — break approved; minimize churn]
- `Configurable.configure(Class<C>, UnaryOperator<C>)` replaces the `Consumer<C>` form (a `Consumer` can't
  express "return the new immutable value"). Cannot overload — a `c -> c.foo()` lambda is ambiguous — so the
  `Consumer` form is **removed** (deprecated first). The ~30 convenience methods keep their exact spelling,
  re-implemented as `configure(RowMappers.class, c -> c.withMapper(m))`.
- **Churn minimized (per sign-off): withers are chainable and there are plural withers.** Each wither returns
  the new immutable value, so migrated lambdas stay single-expression with no `return`:
  `c -> c.setX(v)` → `c -> c.withX(v)`; `c -> { c.setA(a); c.setB(b); }` → `c -> c.withA(a).withB(b)`;
  `factories.forEach(c::register)` → `c -> c.withArgumentFactories(factories)`. Block lambdas with a bare
  `return c` are the thing to avoid; provide a plural/collection wither wherever the old code looped.
- **Break:** direct `configure(X.class, c -> …)` callers only (~38 test + a few main); convenience callers
  unaffected.

### D3 — `ConfigRegistry` derivation primitive
- `ConfigRegistry.with(Class<C>, UnaryOperator<C>)` → a *new* registry with that one value replaced
  (structural sharing once values are immutable). `get(Class)` unchanged (reads). `createCopy()` retired once
  nothing deep-copies. This atom backs the builder, the open-scope, and statement copy-on-write.

### D4 — Jdbi assembly via a builder (primary), `create()` deprecated-but-working
```java
Jdbi jdbi = Jdbi.builder(dataSource)          // or builder(connectionFactory) / builder(url) …
        .installPlugin(new SqlObjectPlugin())
        .registerRowMapper(new FooMapper())
        .configure(SqlStatements.class, c -> c.withTemplateEngine(engine))
        .transactionHandler(handler)          // non-config knobs live on the builder too
        .build();                             // → immutable Jdbi (frozen ConfigRegistry)
```
- The builder is `Configurable<Builder>` (assembly) plus the non-config knobs. `build()` freezes.
- **Compat:** `Jdbi.create(...)` stays; the returned Jdbi accepts the legacy `registerX`/`configure`/
  `installPlugin` (now `@Deprecated`) and freezes on first `open()`. Deprecated mutation is backed by
  derive-and-re-reference (the reverted swap returns *here only*, as scaffolding). Old code keeps working.

### D5 — Plugins
- Add `JdbiPlugin.configure(Jdbi.Builder)` (contributes config + knobs at build). Deprecate
  `customizeJdbi(Jdbi)`. `customizeHandle(Handle)` / `customizeConnection(Connection)` stay (per-handle/
  connection concerns); handle-config contributions route through the open-scope derivation.

### D6 — Handle config largely eliminated; `open(scope)` only if it earns its place [REFINED per sign-off]
Sign-off observation: with Jdbi-level config (build) and statement/template-level config, **handle-level config
has almost no remaining value.** Its one distinct use is an ad-hoc unit of work that runs several *plain*
queries sharing non-global config — covered immutably by `jdbi.open(scope)`, which is a config *choice at open*,
not mutable handle state:
```java
Handle h = jdbi.open();                                              // handle references a copy of Jdbi's config
Handle h = jdbi.open(cfg -> cfg.configure(RowMappers.class, r -> r.register(m)));   // per-handle derived config
```
**D6 (window slice) DONE (2026-07-18).** The additive `open(scope)` forward API landed; the removal-gated pieces
(handle-COW #2992 warm cache, handle-config removal) are the REMOVAL phase in THIS branch (see the plan up top under
"REMOVAL phase"). Decisions (maintainer-confirmed):
- **`open(scope)` earns its place — ADDED, additive `@Alpha`.** `Jdbi.open(Consumer<ConfigRegistry> configScope)` +
  scoped `withHandle`/`useHandle`/`inTransaction`/`useTransaction(Consumer<ConfigRegistry>, …)`. Scope type is
  `Consumer<ConfigRegistry>` (not `UnaryOperator`): `ConfigRegistry.configure` mutates a copy in place and returns
  `this`, so the honest model is "configure this handle's private copy", and it forecloses the footgun of returning
  an unrelated registry. `Handle`'s constructor applies the scope to the `createCopy()` **before** deriving the
  extension context and copying `Handles` listeners, so a scoped `handleCreated` listener is honored. The scoped
  `withHandle`/… always open a NEW handle (a handle captures config at open) and do not join a handle already in
  scope — documented; they save/restore the ambient `handleScope` rather than clearing it.
- **Internals are a facade now, COW later (mirrors D4a).** `open(scope)` = `createCopy()` + apply scope. The public
  signature is independent of the mechanism, so the removal-time swap to `createChild()` COW + #2992 warm cache
  needs no API change.
- **Handle config NOT deprecated in this window (DEFERRED to removal).** `handle.registerX`/`configure` stay
  un-deprecated for now — same reasoning as the D4b.2 register*/configure deferral (a ~120-site in-repo warning
  flood for methods that go away wholesale at removal). At removal they become the removed surface, with `open(scope)`
  as the migration target that now exists.

### D7 — Statement / template config: surface unchanged
- `query.registerRowMapper(m).mapTo(X)` stays. Backed by a lazily-created private derived registry
  (copy-on-first-write), frozen at execute. Statements keep `Configurable`. This is what makes sub-step 5
  clean: no additions → the statement shares the handle's immutable config (no copy); additions → exactly one
  derived registry. The per-statement `createCopy()` at `BaseStatement.java:36` goes away.

### Sign-off checklist (SIGNED OFF 2026-07-17)
- [x] **D1** immutable `JdbiConfig`, drop `createCopy`/`setRegistry`, mutators → withers (breaks custom configs).
- [x] **D2** `configure` `Consumer` → `UnaryOperator` — break approved; churn minimized via chainable + plural withers.
- [x] **D3** add `ConfigRegistry.with(Class, UnaryOperator)`.
- [~] **D4** signed off. **D4a DONE** (`d5565bf13`, additive, no breaks): `Jdbi.builder(...)` factories + `Jdbi.Builder` (`Configurable` + knobs `transactionHandler`/`statementBuilderFactory`/`handleCallbackDecorator`/`handleScope`) + `build()`, a thin assembly facade over `Jdbi.create(...)`. **D4b.1 DONE** (`674f6c496`, `661334b55`): 17 in-repo plugins moved to `configure(Builder)`; `Jdbi.installPlugin` bridges through a throwaway `Builder` so all install sites keep working (no freeze-on-open, no shim — both dropped). **D4b.2 DONE** (`8f69b9f4e`): `@Deprecated(since="4.0.0", forRemoval=true)` on the DIRECTLY-OWNED surface (`installPlugin` + 4 `setX` knobs) + `JdbiPlugin.customizeJdbi`; central test support routed through the builder. The `Configurable.configure` funnel + `register*` as inherited by `Jdbi` were DEFERRED to removal (no override-stubs; they drop with `Configurable`'s mutators). Incidental leaf warnings left (non-fatal); only `TestJdbiBuilder`/`TestPlugins` suppressed. Removal is the REMOVAL phase in THIS branch (4.x), next.
- [~] **D5** signed off. **DONE** (`d5565bf13` + `661334b55` + `8f69b9f4e`): added `JdbiPlugin.configure(Jdbi.Builder)` (default no-op), applied by `build()`/`installPlugin` before `customizeJdbi` in install order; all in-repo plugins migrated onto it (D4b.1); `customizeJdbi(Jdbi)` deprecated forRemoval (D4b.2).
- [~] **D6** window slice DONE (2026-07-18): additive `@Alpha` `Jdbi.open(Consumer<ConfigRegistry>)` + scoped `withHandle`/`useHandle`/`inTransaction`/`useTransaction` (facade = `createCopy()`+apply scope; COW #2992 deferred to removal). Handle-config deprecation (`handle.registerX`/`configure`) DEFERRED to removal (like the D4b.2 register*/configure deferral). `open(scope)` is now the migration target for handle-config removal.
- [x] **D7** statement-level `Configurable` unchanged (copy-on-write private config).

**Implementation order once signed off:** D3 (additive) → D1+D2 per config domain (withers + convenience
re-impl, like sub-step 2's domain-by-domain cadence) → D7 (statement copy-on-write) → sub-step 5 (delete the
per-statement copy, re-benchmark) → D4/D5/D6 (the builder + deprecations + open-scope) last, since they are the
largest public surface and depend on the value/registry immutability being in place.

**Naming (per sign-off):** config-value building methods are prefix-free — `register(...)` keeps its name
(already returns the config type); scalar setters become `templateEngine(e)` / `keyColumn(k)` (no `set`, no
`with`), consistent with `register` and clash-free with `getX()`/`isX()` getters. Apply during D1.

## D4b proposal (2026-07-18): freeze the Jdbi config by REMOVING post-construction mutation (deprecate → remove)

This is the first design pass for D4b. D4a already landed the additive `Jdbi.builder()` + `JdbiPlugin.configure(Builder)`.
D4b makes the builder the *only* way to configure a `Jdbi`, so a built `Jdbi`'s config is immutable for its life.

### Decision: deprecate → remove; NO compatibility shim; NO freeze-on-open mechanism
The earlier sketch proposed a derive-and-re-reference *shim* plus *freeze-on-open*. Both exist only to run
immutable-config and legacy in-place mutation **at the same time**. If we instead remove the mutation API outright,
neither is needed:
- **During the deprecation window**, the deprecated mutators keep their *current* behavior — in-place mutation of
  the `Jdbi`'s registry. That is already safe: `install()` clears memoized views (D7), and a handle takes
  `createCopy()` of the config at `open()`, so post-open `jdbi.registerX` only affects *newly* opened handles
  (the long-standing status quo). Nothing new to guarantee.
- **At removal**, the mutating methods are gone, so a `Jdbi` assembled by the builder is immutable by construction.
  No runtime "frozen" flag, no `IllegalStateException("read only")`.
- **No shim is justified.** The shim bridges "config is already immutable but the old mutation API is still present";
  we never enter that state. (If review wants immutability *enforced during* the window — e.g. to unlock handle-level
  COW early — the minimal tool is throwing from the deprecated mutators after first `open()`; that is a behavior
  break, heavier than plain deprecation, and can wait for the removal step. Recommend: do not.)

### Target end state
- Assemble with `Jdbi.builder(...)…build()`. The built `Jdbi`'s config never changes.
- **`Configurable` splits into read vs mutate.** The read surface already exists as `ConfigReader`
  (`getConfig()` + `findMapper`/`findArgument`/… ; `ConfigRegistry` implements it). At removal, `Jdbi` implements the
  read surface only; the mutate surface (`configure`/`register*`) stays on the scoped-then-frozen owners: the
  **builder** (assembly) and **statements/templates** (per-execution, COW). Handle is decided in D6.
- **`Jdbi.create(...)` stays** (CONFIRMED with maintainer 2026-07-18), un-deprecated, as the zero-config shortcut —
  semantically `builder(x).build()`. This revises the earlier "create() deprecated" sign-off: create() is the
  universal, ergonomic entry and the zero-config path is legitimate; only post-construction *configuration* goes away.
  Migration is `Jdbi.create(ds).installPlugin(p).registerX(...)` →
  `Jdbi.builder(ds).installPlugin(p).registerX(...).build()`.
- **Plugins** contribute via `configure(Jdbi.Builder)`; `customizeJdbi(Jdbi)` is removed. `customizeHandle(Handle)` /
  `customizeConnection(Connection)` stay (per-handle/connection concerns).
- **A lambda shorthand for build-time config (CONFIRMED — add):** `JdbiPlugin.of(Consumer<Jdbi.Builder>)`, a static
  factory whose `configure(Builder)` runs the consumer, so a small config bundle needs no anonymous `JdbiPlugin`:
  `builder.installPlugin(JdbiPlugin.of(b -> b.registerImmutable(Foo.class).registerRowMapper(m)))`. It implements
  **only** `configure(Builder)` (never `customizeJdbi`): the builder's `build()` runs both hooks, so implementing
  both would apply the consumer twice; and the go-forward path is the builder. This is what the anonymous-plugin
  test sites from `fb4e06797` (immutables migration) should collapse to. For the tightest test ergonomics, the test
  extensions can also expose a thin `withConfig(Consumer<Jdbi.Builder>)` convenience (wrapping
  `withPlugin(JdbiPlugin.of(...))`) during the D4b.1 extension migration.

### Surface to deprecate (D4b) → remove (this 4.x branch, REMOVAL phase)
- `Jdbi.installPlugin`, `Jdbi.setTransactionHandler`, `setStatementBuilderFactory`, `setHandleCallbackDecorator`,
  `setHandleScope` (builder has all of these).
- The `Configurable` mutators *as inherited by `Jdbi`*: `configure(Class, UnaryOperator)` and the ~30 `register*`
  conveniences. **Granularity (DECIDED 2026-07-18, per maintainer):** mark the `configure` funnel + the directly-owned
  methods + the highest-traffic `register*` overloads (`registerRowMapper`/`registerColumnMapper`/`registerArgument`)
  `@Deprecated`; let the rest fall away when `Jdbi` drops `Configurable` at removal. Do NOT write ~30
  override-just-to-deprecate stubs on `Jdbi`. (Deprecating the funnel alone does not warn callers of
  `jdbi.registerRowMapper(...)` — sibling default methods — hence deprecating the top overloads too.)
- `JdbiPlugin.customizeJdbi(Jdbi)`.

### Migration plan (staged; each stage whole-reactor green and warning-clean)
The build tolerates `@Deprecated(forRemoval=true)` declarations (see `Configurable:124`), so ordering is flexible;
migrate-first keeps the tree clean and is recommended.
1. **D4b.1 — DONE (`674f6c496`, `661334b55`). Migrate in-repo plugins to `configure(Builder)`; bridge legacy
   `installPlugin` (behavior-preserving, no `@Deprecated` yet).** Two commits:
   - **Infrastructure (`674f6c496`):** `build()` drains plugins by index (a plugin's hook may install further
     plugins mid-drain); `Builder.installPlugin` is add-if-absent; new `JdbiPlugin.of(Consumer<Jdbi.Builder>)`
     lambda shorthand (implements `configure(Builder)` only).
   - **Migration + bridge (`661334b55`):** moved config in all 17 in-repo plugins from `customizeJdbi(Jdbi)` to
     `configure(Jdbi.Builder)` (SqlObject/Postgres/Postgis/Oracle/SQLite/Json/Jackson2/Jackson3/Gson2/Moshi/Guava/
     Vavr/JodaTime/Jpa/OpenTelemetry/H2/ConfiguringPlugin); left `customizeHandle`/`customizeConnection` alone.
   - **The bridge (revises the doc's original "pure move + migrate ~47 sites" plan).** A pure move would silently
     break `Jdbi.create(ds).installPlugin(inRepoPlugin)` during the very window meant to keep it working, since
     `configure(Builder)` only runs via `build()`. Instead, `Jdbi.installPlugin` now delegates to
     `new Builder(this).installPlugin(plugin).build()`. A `Builder` is a facade over the same `Jdbi` (shared
     `ConfigRegistry`), so this applies the `configure(Builder)` hook even on the post-construction path. Result:
     **every existing `installPlugin` call site keeps working with no lockstep migration** — the test support
     (`JdbiExtension:326` = `plugins.forEach(jdbi::installPlugin)`) and the ~47 assembly sites were left untouched
     and stayed green. A shared private `Jdbi.applyPlugin(Builder, plugin)` funnel runs `configure()` then
     `customizeJdbi()` once per plugin; `build()` and `installPlugin()` both route through it. **The bridge is
     disposable:** at removal, deleting `Jdbi.installPlugin` takes the one-line delegation with it and leaves
     `applyPlugin()`/`build()` as the permanent assembly funnel.
   - **Consequence for the doc's ~47-site migration:** no longer required for correctness. Converting call sites
     from `create(x).installPlugin(p)…` to `builder(x).installPlugin(p)…build()` is now optional cosmetic cleanup
     that can ride along with the D4b.2 deprecation (deprecating `installPlugin` will flag those sites).
2. **D4b.2 — DONE (`8f69b9f4e`). Deprecated the DIRECTLY-OWNED post-construction mutation surface.**
   `@Deprecated(since = "4.0.0", forRemoval = true)` on `Jdbi.installPlugin`/`setTransactionHandler`/
   `setStatementBuilderFactory`/`setHandleCallbackDecorator`/`setHandleScope` + `JdbiPlugin.customizeJdbi(Jdbi)`,
   each javadoc pointing at its `Jdbi.Builder`/`configure(Builder)` replacement; `applyPlugin` keeps honoring
   `customizeJdbi` during the window under a scoped `@SuppressWarnings("deprecation")`. Central test support routed
   through the builder: `testing/JdbiExtension` + core-test `H2/Pg/Sqlite DatabaseExtension` assemble via
   `Jdbi.builder(...).build()`, and `DatabaseExtension.installTestPlugins()` takes a `Jdbi.Builder`.
   **Two scope decisions (maintainer-confirmed 2026-07-18), revising the doc's original plan:**
   - **`Configurable` mutators as inherited by `Jdbi` (`configure` funnel + top `register*` overloads) NOT deprecated
     here — DEFERRED to removal.** The `@Override`-just-to-deprecate stubs would add ~6 stubs + ~7 imports to `Jdbi`
     and a large test-warning flood; the directly-owned deprecations already signal the direction, and these methods
     fall away wholesale when `Jdbi` drops `Configurable`'s mutators at removal (read-only split).
   - **Leaf-site cleanup: suppress-intentional, leave-incidental.** javac `-Xlint:deprecation` warnings are NON-FATAL
     and already pervasive in-repo (e.g. `NamedArgumentFinder.find` used post-deprecation in MAIN source), so the
     ~66 incidental leaf callers (`create(x).installPlugin(p)`, `setX`, test-fixture `customizeJdbi` overrides) are
     left to warn and get swept at removal — consistent with existing convention. Only the two tests whose SUBJECT is
     the deprecated install path (`TestJdbiBuilder`, `TestPlugins`) carry a class-level `@SuppressWarnings`.
3. **REMOVAL phase (this 4.x branch — see the sequenced R1–R5 plan up top under "REMOVAL phase"):** delete the
   deprecated methods (the `installPlugin` bridge disappears with them); `Jdbi` drops `Configurable`'s MUTATORS for the
   read-only surface — this is where the deferred `configure`/`register*` removal lands (no per-method deprecation
   stubs were ever written; the whole mutate surface goes at once), and where the ~66 incidental leaf callers + the
   pre-existing `create(...).installPlugin(...)` sites are converted to the builder in one sweep. Then handles become
   `createChild()` COW off the now-immutable `Jdbi` config — the jdbi/jdbi#2992 warm-cross-handle metadata fix (see
   that note). Handle-level mutable config is retired in the same phase (R4).

### Implementation notes / gotchas to carry in
- **`build()` drains dynamically-added plugins (DONE in `674f6c496`).** `build()` iterates by index via a `while`
  loop (not a for-each — PMD's `ForLoopCanBeForeach` flags an index `for`), so once a plugin's `configure(builder)`
  (or a legacy `customizeJdbi`) calls `builder.installPlugin(sub)`, the loop picks `sub` up. `applyPlugin`'s
  install-if-absent guard makes a plugin pulled in by two others apply once. No `ConcurrentModificationException`.
- **Plugin ordering & dedup**: preserved — install order; `applyPlugin`/`installPlugin` install-if-absent so a plugin
  pulled in by two others runs once.
- **Validation gotcha:** the plugin modules depend on the modified `core`. Run their tests with `-am` (or install
  `core` first) — `mvn … -pl guava` alone compiles the migrated plugin against the **stale installed core** whose
  `installPlugin` predates the bridge, so `configure()` never runs and every registration silently no-ops.
- **Handle-level config is out of scope here** (`handle.registerX`); that is D6. D4b is Jdbi-level only.
- **Third-party plugins** that only implement `customizeJdbi` keep working through the deprecation window (the bridge
  runs `customizeJdbi` too) and break at removal — that is the window's purpose; call it out in release notes.

## Tasklist

### 1. Baseline & measurement — DONE (the win is real, proven by JMH `-prof gc`)
Allocation/op is the deterministic metric in this container (throughput is noisy, ±50%).
- **Fluent path** (`QueryTemplateBenchmark`, single-row SELECT mapped to String): classic
  `createQuery` **8,168 B/op** → reused `QueryTemplate` **3,816 B/op** (~53% less, ~2.1×; ~2.7× throughput).
  Hoisting render+parse to build time took the template to **~3.5 KB/op** (a further ~9%).
- **SQL Object** (`H2SqlObjectV3Benchmark.sqlobjectSelectOne`): classic **~9,100 B/op** → retargeted onto the
  template **~4,334 B/op** (~52%, ~2.1×); the ~0.5 KB over the pure fluent template is extension dispatch +
  `ResultReturner`.
- **Where the remaining template allocation goes** (JFR `ObjectAllocationSample`, weight-based — a count-based
  first pass overstated the small-object render/cache share): ~1/3 is the H2 driver itself (the in-mem floor,
  dwarfed by I/O on a real DB); the controllable jdbi remainder — candidates for the phase-3 incremental trims —
  is `ArgumentFactoryLocator` build (~12%), `Binding` + its `HashMap`s (~11%), `ResultSetSupplier` closing
  context (~7%), per-execute `Instant` timing (~5%), `SingleColumnMapper` build (~4%), `StatementContext` +
  cleanables (~4%). No single dominant lever remains.

> **Build notes (JDK 26 env):** (1) The JMH annotation processor must be declared in
> `annotationProcessorPaths` — done in `benchmark/pom.xml` — because since JDK 23
> `javac` ignores classpath-only processors, so `META-INF/BenchmarkList` was never
> generated. (2) Local repo is `/var/cache/maven-repository`, not `~/.m2`.
> (3) `mvn install` must be run with `clean` — a no-clean run collides in the
> `inline` plugin on a duplicate antlr `MANIFEST.MF` from stale artifacts.
> (4) Build the benchmark with `mvn clean package -pl benchmark -am -DskipTests
> -Dbasepom.check.skip-all=true` then run `java -jar benchmark/target/benchmarks.jar
> "<class>.<method>" -f 1 -wi 2 -i 4 -prof gc`.

### 2. Config contract redesign — DONE (all items landed via the immutable-config + handle-COW work)
- [x] Complete the field taxonomy table above (reviewed against current sources 2026-07-17;
      added the `extensionState` slot and the post-retarget note).
- [x] Immutable config snapshot type + `configure(callback)` scoped mutation. Landed as immutable
      `JdbiConfig` values + `configure(Class, UnaryOperator)` + `ConfigRegistry.createChild()`
      copy-on-write. A customizer that mutates config without the `ConfigMutating` marker no longer
      corrupts the shared snapshot; the late path forks copy-on-write instead of falling back to the
      whole classic path.
- [x] Handle config immutable after construction. `Handle` implements read-only `ConfigReader` (R4)
      and its config is a `createChild()` copy-on-write child of the frozen `Jdbi` root (R3), so the
      classic fluent path also skips the per-statement `createCopy()`.
- [x] Move statement-state fields off config/context onto the statement/binding (per taxonomy).
      Per-invocation state moved onto an opaque `StatementContext` extension-state slot and the
      binding; the customizer phase model keeps invariant config in the snapshot.
- [x] Split defines out of the shared config. `TemplateEngine.render/parse` take a `RenderContext`
      (config + a per-execution defines overlay) instead of a raw `ConfigRegistry`.

### 3. Template primitive
- [x] Real `execute()`: fresh binding/context/statement per call, handle-owned
      cleanup, nothing retained on the template between executions. (Minimal slice;
      still renders/parses per call — see hoisting below.)
- [x] Hoist render + parse to build time. `QueryTemplate` renders and parses once in
      its constructor and stores `renderedSql` + `ParsedSql`; `QueryTemplateBinding`
      reuses them and no longer calls `preparedRender`/`getSqlParser().parse` per execute.
      ~9% allocation drop (see phase 1).
- [x] Per-execution defines overlay → re-render path. `QueryTemplateBinding.define`
      records overrides in an overlay; execution reuses the hoisted SQL when the overlay
      is empty, and re-renders/re-parses with `RenderContext(config, overlay)` when not.
      `define()` is now a real abstract method (`Definable<This>`) instead of a stub that
      threw; classic statements define into config, and `EmptyHandling`/`bindList` work
      uniformly on both via `Definable`. Full core suite (1003) + all template modules green.
- [ ] `reconfigure(callback)` → new template.
- [ ] Encode the thread-confinement boundary in types + javadoc.
- [ ] **Incremental allocation trims (from the weight-based profile; do opportunistically):**
      - Hoist argument-factory / mapper resolution to build time where the parameter and
        result types are known (`ArgumentFactoryLocator` ~12%, `SingleColumnMapper` ~4%
        are rebuilt every execute).
      - Lighter `Binding` for a small number of parameters (avoid a full `HashMap`
        per execute; ~11%).
      - Avoid per-execute `Instant` timing allocation when no telemetry/logger observes it
        (~5%).
      - Trim `ResultSetSupplier` closing-context machinery (~7%).
      None is a large lever; the H2 driver (~1/3) is the practical floor for in-mem.

### 4. Cache unwinding
- [ ] Memoize resolution on the immutable config instance (eager at build).
- [ ] Remove the config-entangled global caches.
- [ ] Decide whether to keep a single pure `SQL → parsed-fn` cache for classic
      one-shot use; if not, remove it too.
- [ ] **`JdbiCache.getWithLoader` is allocation-heavy per lookup** — the allocation
      profile showed `TreeMap`/`KeySet`/`KeyIterator`/`HashMap$Node` churn *inside*
      each cache access, plus callers (`preparedRender`) build a fresh loader lambda
      and `StatementCacheKey` on every call even on a hit. This taxes the classic path
      too (templates bypass it once render is hoisted). Investigate the default cache
      impl (`DefaultJdbiCacheBuilder`) and the loader-per-call pattern; make hits
      allocation-free.

### 5. Unify the APIs
- [x] **DONE (2026-07-17):** Retarget SQL Object onto the template — the full breaking refactor.
      Introduced `Customizable` (supertype of `SqlStatement` and `QueryTemplateBinding`), retyped the
      customizer SPI (`SqlStatementCustomizer`/`SqlStatementParameterCustomizer` take `Customizable<?>`)
      and made the handler base non-generic over `Customizable<?>`, moved `args`/`returner` off the
      (deleted) `SqlObjectStatementConfiguration` onto an opaque `@Alpha` `StatementContext` slot, and
      `SqlQueryHandler` now builds one `QueryTemplate` per attach. **Config mutation was solved by the
      customizer phase model** (see the "Customizer phase model" section): configure-phase customizers bake into the template
      snapshot once; bind-phase customizers only touch the per-execution binding; `ConfigMutating` (late)
      customizers fall back to the classic per-statement path. Two latent bugs fixed en route: the binding
      used a separate `Binding` from `ctx.getBinding()` (broke `beforeBinding` customizers), and it skipped
      the `null`-result-set → `NoResultsException` check. `@Timestamped` reworked to a configure-registered
      `StatementCustomizer`. Full reactor green (core 1006, sqlobject 499, all modules).
- [x] Re-ran the SQL Object benchmark before/after (~9,100 → ~4,334 B/op) — numbers in phase 1 above.
- [ ] Reimplement `Handle.createQuery`/`createUpdate`/… on the primitive (the remaining phase-5 tail; this is
      what lets the classic fluent path share config with no copy, and where `buildQueryTemplate`/`QueryTemplate`
      naming can be revisited).

### Open API decisions — RESOLVED (2026-07-17)
- **`Jdbi.buildQueryTemplate` name:** kept (not renamed to `buildQuery`). The name matches its return
  type `QueryTemplate` and stays distinct from the one-shot `Handle.createQuery`; least-surprise wins.
  Revisit if/when `Handle.createQuery` is reimplemented on the primitive (the remaining phase-5 item).
- **`QueryTemplate(ConfigRegistry, CharSequence)` constructor visibility:** kept public. It is the entry
  point the sqlobject module uses to build a template from a method-level config snapshot (cross-module),
  and is a legitimate advanced API alongside `Jdbi.buildQueryTemplate`; documented in javadoc.

### 6. Finish
- [ ] Tests: reuse across handles and threads, binding, constant + dynamic defines,
      `reconfigure`, cleanup/resource lifecycle, error paths.
- [ ] Javadoc edit pass. **Convention: javadoc describes how things are *now*.** Do not
      compare to or reference the older (to-be-deleted) jdbi3 behavior, and avoid framing
      like "unlike before" / "previously this copied config". Describe the current API and
      its behavior directly. (Applies to the new `QueryTemplate*` types — trim the
      comparative "avoids what the classic path pays" phrasing to a plain description.)
- [ ] Fold this document's content into javadoc + user guide.
- [ ] Delete this file.
