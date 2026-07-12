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

## Field taxonomy (central deliverable — complete in task 2.1)

Every field currently in `StatementContext` / `SqlStatements` / config gets a home.
Inventoried from the actual sources (needs a final review pass, task 2.1):

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

## Open API-surface decisions

Guiding principle: the public API should be as small as possible and as large as
needed — one obvious way to do each thing, no leaked internals. Two open questions:

- [ ] **Rename `buildQueryTemplate` → `buildQuery`?** Once the classic query paths are
      retired and templates are the only way to build a query, the word "template" is
      redundant qualification. `jdbi.buildQuery(sql)` reads better and is the one obvious
      entry point. Counter-consideration: the returned type is `QueryTemplate` (reusable,
      not a one-shot `Query`), so `buildQuery` returning a `QueryTemplate` may mislead;
      resolve the method name and the type name together. Decide during phase 5 (unify),
      not piecemeal — renaming twice churns the API.
- [ ] **Make the `QueryTemplate` constructor non-public.** Right now it is `public`
      `QueryTemplate(ConfigRegistry, CharSequence)` so `Jdbi.buildQueryTemplate` (a
      different package) can call it, and so the SQL Object retarget can build templates
      from an extension's config later. Neither reason requires it to be *public API* for
      users — the single intended entry point is `jdbi.buildQueryTemplate(...)`. Constraint:
      `QueryTemplate` is in `org.jdbi.core.statement`; both callers are cross-package
      (`org.jdbi.core.Jdbi`; the SQL Object retarget lives in the `sqlobject` module), so a
      package-private constructor will not compile without a construction seam. Options:
      (a) keep it public (simplest, matches the public `Query(Handle, CharSequence)`
      constructor); (b) introduce an internal factory both callers use and hide the
      constructor. Resolve alongside the SQL Object retarget (step 3), when the second
      caller's construction path is actually designed.

## HANDOFF (2026-07-12, end of day): phase 5 SQL Object retarget — START HERE

**Read this first if picking up fresh.** The row-reducer result-model change is DONE; the
live front is the SQL Object retarget (phase 5 unify). Decision made with the maintainer:
**do the full refactor** (option #1 below) — breaking the public customizer SPI is
sanctioned for v4. Implement it next session.

### State (all committed on branch `query-templates`, all green)
Commits this session (newest last): `d0a0fc90` binding is a `ResultBearing` · `027544dd`
per-execution customizer surface · `8df58eb` blocker analysis docs. Earlier: `bd0e56a2`
hoist render/parse · `55c5471` TemplateEngine→RenderContext · `9c146cd` wire define() ·
`2773934` Definable/EmptyHandling.

- **Step 1 DONE** (`d0a0fc90`): `QueryTemplateBinding implements ResultBearing` via the
  single `scanResultSet` primitive → `mapTo`/`map`/`reduceRows`/`collectInto`/`stream` all
  work per-execution, identically to `Query`. `QueryTemplate` is non-generic (SQL + config
  snapshot + hoisted rendered SQL + `ParsedSql`); `Jdbi.buildQueryTemplate` returns it.
  `QueryTemplateBuilder`/`QueryTemplateBuilderImpl` and the baked scanner are deleted.
- **Step 2 DONE** (`027544dd`): binding has `setFetchSize`/`setMaxRows`/`setMaxFieldSize`/
  `setQueryTimeout`/`addCustomizer`/`getContext`, records customizers binding-locally (not
  in the shared config), and invokes the `beforeTemplating`/`beforeBinding`/`beforeExecution`/
  `afterExecution` hooks — which the template path had never invoked (latent bug, now fixed).

Verified: **core 1006 tests / 0 failures**; benchmark compiles (`.with(h).mapTo(String).one()`).
Build notes: JDK 26 env, local repo `/var/cache/maven-repository`; validate with
`mvn clean verify -pl core -Dbasepom.check.skip-all=true` (must include `clean` — a no-clean
run collides on a duplicate antlr `MANIFEST.MF` in the inline plugin). **Pre-existing PMD
debt** on this branch (8 violations in unrelated files — `Definable`, `QueryCustomizerMixin`,
`SqlStatements`, `MapArguments`, etc.) surfaces if you drop `-Dbasepom.check.skip-all=true`;
it predates this session's work (prior sessions always skipped checks) and is not from the
step 1/2 changes. Clean it up in a separate pass; don't fold it into the retarget commit.

### The plan for next session (ordered, mirrors the recommended design below)
1. **Introduce `Customizable`** (new interface, `org.jdbi.core.statement`). It is the
   customizer-facing surface: extends `BindingsMixin<This>` (→ `bind*`, `define*`,
   `getConfig`) and adds the tail customizers actually use: `StatementContext getContext()`,
   `This addCustomizer(StatementCustomizer)`, `This setQueryTimeout(int)`,
   `This attachToHandleForCleanup()`. (Self-typed `<This>`; SPI methods take `Customizable<?>`.)
2. **`SqlStatement implements Customizable<This>`** — it already has every method (verify:
   `getContext`/`attachToHandleForCleanup` on `BaseStatement`, `setQueryTimeout`/`addCustomizer`
   already present). Likely zero body changes, just the `implements` clause.
3. **`QueryTemplateBinding implements Customizable<QueryTemplateBinding>`** — it has all but
   `attachToHandleForCleanup`; add an analog (register the ctx/statement for cleanup when the
   handle closes; see `BaseStatement.attachToHandleForCleanup` at `BaseStatement.java:72`).
4. **Retype the two public SPI methods** `SqlStatementCustomizer.apply(Customizable<?>)` and
   `SqlStatementParameterCustomizer.apply(Customizable<?>, Object)`. Fix the **8** explicit
   `apply(SqlStatement…)` signatures repo-wide (5 in sqlobject; grep
   `apply(SqlStatement\|apply(final SqlStatement`). Lambdas re-infer for free.
5. **Widen `CustomizingStatementHandler`** — generic bound
   `<StatementType extends SqlStatement<StatementType>>` → `<StatementType extends Customizable<StatementType>>`
   and `BoundCustomizer.apply(SqlStatement<?>, …)` → `Customizable<?>`. All four handlers
   (Query/Update/Batch/Call) still satisfy it since `SqlStatement implements Customizable`.
6. **Move per-invocation `args`/`returner` off `SqlObjectStatementConfiguration`.** Today
   `attachTo` does `stmt.getConfig(SqlObjectStatementConfiguration.class).setArgs(args)` +
   `.setReturner(...)` + `.getReturner().get()`. This mutates config — safe only with a
   per-statement config copy, unsafe under a shared template snapshot. Move `args`/`returner`
   into per-call state (e.g. fields on the created statement/binding, or a small per-call
   holder threaded through `attachTo`). Note: `ResultReturner.ConsumerResultReturner`/
   `FunctionResultReturner.findConsumer/findFunction` read args via
   `ctx.getConfig(SqlObjectStatementConfiguration.class).getArgs()` — update those read sites too.
7. **Retarget `SqlQueryHandler`**: build one `QueryTemplate` per method (at `attachTo`/warm
   time, from the method's config), and `createStatement` returns a fresh
   `template.with(handle)` binding per call. `configureReturner` already only needs
   `mapTo`/`map`/`reduceRows` on the binding (it's a `ResultBearing`) → `ResultReturner`
   reused unchanged. Do `SqlUpdateHandler` etc. only if in scope; Query first.
8. **Resolve the two [Open API-surface decisions](#open-api-surface-decisions)** while here
   (rename `buildQueryTemplate`→`buildQuery`; `QueryTemplate` constructor visibility).
9. **Validate**: full `core` + `sqlobject` suites green; then benchmark SQL Object
   before/after and record numbers in phase 5.

Risk: this is all-handlers-at-once (shared base) and a breaking public SPI. Expect it to
touch ~15-20 files. If it doesn't converge cleanly, land steps 1-3 (the non-breaking
`Customizable` extraction) first, then the SPI retype separately.

### Related piece for the SQL Object retarget (DONE)
Query customizers (`@FetchSize`, `@MaxRows`, …) call `QueryCustomizerMixin` methods not on
the binding. Add that surface to `QueryTemplateBinding` (apply as statement-state at
execute time). **DONE (commit `027544dd`)** — the binding now has
`setFetchSize`/`setMaxRows`/`setMaxFieldSize`/`setQueryTimeout`/`addCustomizer`/`getContext`,
records customizers locally (not in the shared config), and invokes the
`beforeTemplating`/`beforeBinding`/`beforeExecution`/`afterExecution` hooks (which the
template path had never invoked — a latent bug).

### SQL Object retarget — precise blocker analysis (2026-07-12)
`ResultReturner` is already decoupled: it consumes a `ResultIterable<?>` / `Stream<?>` /
`StatementContext`, all of which `QueryTemplateBinding` produces (it's a `ResultBearing`).
So the returner side is a non-issue. The entire remaining coupling to `SqlStatement` is
**two interlocking things**, and they cannot be split apart because all four statement
handlers share one base:

1. **The public customizer SPI is hard-typed to the concrete `SqlStatement<?>`.**
   `SqlStatementCustomizer.apply(SqlStatement<?>)` and
   `SqlStatementParameterCustomizer.apply(SqlStatement<?>, Object)` (both public, both
   implemented by external users) pass the statement to arbitrary customizers. The default
   `@Bind` customizer (`BindFactory`) calls `stmt.getConfig()` + `stmt.bindByType(...)` —
   all on `BindingsMixin`, which the binding has. The full method surface customizers use is
   `bind*`/`define*`/`getConfig` (BindingsMixin) plus a small tail: `getContext` (×1),
   `setQueryTimeout` (×2), `attachToHandleForCleanup` (×1). Only **8** explicit
   `apply(SqlStatement…)` signatures exist repo-wide (5 in sqlobject) — lambdas re-infer
   for free. So a shared `Customizable` interface (a supertype of both `SqlStatement` and
   `QueryTemplateBinding`, carrying that surface) + retyping the two SPI methods is
   mechanically small, but it is a **breaking change to a widely-implemented public SPI**.
2. **`CustomizingStatementHandler<StatementType extends SqlStatement<StatementType>>` is
   shared** by `SqlQueryHandler`, `SqlUpdateHandler`, `SqlBatchHandler`, `SqlCallHandler`.
   The generic bound and `BoundCustomizer.apply(SqlStatement<?>, …)` must widen to
   `Customizable` for even one handler to target the binding. That is an all-handlers-at-once
   change, not a per-handler one.
3. **`SqlObjectStatementConfiguration` stores per-invocation state (`args`, `returner`) in
   config.** `attachTo` does `stmt.getConfig(SqlObjectStatementConfiguration.class).setArgs(args)`
   then `.setReturner(...)` then `.getReturner().get()`. That is per-statement-safe today only
   because each classic statement owns a config *copy*. A template binding shares an immutable
   config snapshot, so this per-invocation state must move off config (onto the binding / a
   per-call object) before the retarget is thread-safe. This is the same "state in config"
   anti-pattern the whole redesign removes.

Recommended design (when green-lit): introduce `Customizable` (supertype carrying
`bind*`/`define*`/`getConfig`/`getContext`/`addCustomizer`/`setQueryTimeout`/
`attachToHandleForCleanup`); `SqlStatement implements Customizable` (it already has every
method); ensure `QueryTemplateBinding` implements it (add an `attachToHandleForCleanup`
analog); retype both SPI methods and the handler base to `Customizable`; move
`args`/`returner` out of `SqlObjectStatementConfiguration` into per-call state; then
`SqlQueryHandler.createStatement` builds one `QueryTemplate` per method (at `attachTo` /
warm time) and returns a fresh binding per call. This is the core of phase 5 (unify) and is
**breaking + interconnected** — it is the natural stopping point for a checkpoint before
executing across the module.

### Suggested order for the fresh session
Steps 1 and 2 are DONE (see "State" above). The next-session plan is
"### The plan for next session" near the top of this HANDOFF (the phase 5 retarget);
after it, benchmark SQL Object before/after, then return to phase 2 (immutable `JdbiConfig`).

## Sequencing decision (2026-07-12)

The template win is proven (phase 1) with the `JdbiConfig` contract **unchanged** —
the template snapshots config once at build via the existing `createCopy()`. So the
`JdbiConfig` immutable-contract redesign (phase 2, ~48 impls, breaking) is not on the
critical path for the win; it is for the clean API, extending the win to the classic
path, and cache unwinding. Decision: **do phase 5's SQL Object retargeting next**
(bank the flagship consumer's win now, low risk, no `JdbiConfig` change), then return
to phase 2. Phases below are kept in logical order but executed 1 → 5(SQL Object) → 2 → 3 → 4 → rest.

## Tasklist

### 1. Baseline & measurement (do first — de-risk before the big rewrite)
- [x] Survey the `benchmark/` JMH module. `AbstractSqlObjectBenchmark` +
      `H2SqlObjectV3Benchmark` already exercise the classic per-statement path
      (`fluentSelectOne`, `sqlobjectSelectOne`) against in-memory H2 — that is our
      classic baseline; no new benchmark needed for the "before" number.
- [x] Baseline: classic path, single SELECT (create + bind + map + one), measured
      on `origin/jdbi4-dev` (our branch can't run it — see finding below).
      **Allocation per op (deterministic, `-prof gc`):**
      - `fluentSelectOne` — **~8.4 KB/op**
      - `sqlobjectSelectOne` — **~9.1 KB/op**
      (Throughput is unreliable in this container — ~120-130 ops/ms ±50%. Trust
      allocation/op.) So ~10k statements ≈ 85-90 MB of allocation churn.
- [x] Minimal working `execute()` slice. Wired `QueryTemplateBinding.execute()` to
      render/parse/bind/execute against the shared context, and fixed the
      `BindingsMixin.getConfig()` UOE (now abstract; `QueryTemplateBinding` returns
      the shared config). `TestQueryTemplates` (6 tests) passes.
- [x] Compare. Benchmark `QueryTemplateBenchmark` (classic `createQuery` vs reused
      `QueryTemplate`, same single-row SELECT mapped to String), `-prof gc`:
      | path | alloc/op | throughput |
      | --- | --- | --- |
      | classic | **8,168 B/op** (±2) | 138 ops/ms |
      | template | **3,816 B/op** (±0.7) | 373 ops/ms |
      **~53% less allocation (2.1×), ~2.7× throughput.** This is the minimal slice
      that still re-renders/re-parses per execute; hoisting those (phase 3) lowers
      it further. Confirms the config copy was ~half the per-statement cost. Win is real.
- [x] Allocation attribution (JFR `ObjectAllocationSample`, standalone
      `TemplateAllocProfiler`, 3M executions). **Aggregate by summed weight (bytes), not
      sample count** — a count-based first pass badly overstated the render/cache share
      (small objects, many samples). Weight-based, post-hoist (3.5 KB/op):
      - **~1/3: H2 driver** (`VersionedBitSet`, `JdbcResultSet`, `Snapshot`,
        `ExpressionVisitor`, `JdbcPreparedStatement`) — real query execution under
        `SqlLoggerUtil.wrap`. The unavoidable floor for in-mem H2; dwarfed by I/O on a
        real network DB.
      - Controllable, per-execution jdbi allocation (candidates for further trimming):
        `ArgumentBinder$ArgumentFactoryLocator` build + lambdas (~12%); `Binding` +
        its `HashMap`s (~11%); `ResultSetSupplier` closing-context (~7%); `Instant`
        execution/completion timing (~5%); `SingleColumnMapper` build (~4%);
        `StatementContext` + cleanables (~4%).
      No single dominant lever remains — the rest is spread across inherent per-execution
      objects. Realistic further target is incremental, not ~1 KB/op.
- [x] Hoist render + parse to build time. Result: **3.8 KB/op → 3.5 KB/op (~9%)**,
      deterministic. Smaller than the count-based guess predicted (render/parse cache was
      not the big cost); still correct architecture (render once) and removes per-call
      `JdbiCache` lookups. The bigger remaining wins are the per-execution objects above.

> **Finding (blocks a same-branch baseline):** the WIP refactor broke the classic
> runtime path. `BindingsMixin.getConfig()` (core `.../statement/BindingsMixin.java:68`)
> is an unimplemented stub that `throw new UnsupportedOperationException()`, and the
> bind path (`bindBean` → `bindNamedArgumentFinder` → `getConfig()`) hits it. Both
> the fluent API and SQL Object fail at runtime on this branch (compilation passes
> because tests were skipped). Wiring `getConfig()` correctly on the binding is a
> prerequisite for phase 3 (`execute()`) and phase 5 (unify). It also confirms this
> is a correctness fix, not only a perf change.

> **Build notes (JDK 26 env):** (1) The JMH annotation processor must be declared in
> `annotationProcessorPaths` — done in `benchmark/pom.xml` — because since JDK 23
> `javac` ignores classpath-only processors, so `META-INF/BenchmarkList` was never
> generated. (2) Local repo is `/var/cache/maven-repository`, not `~/.m2`.
> (3) `mvn install` must be run with `clean` — a no-clean run collides in the
> `inline` plugin on a duplicate antlr `MANIFEST.MF` from stale artifacts.
> (4) Build the benchmark with `mvn clean package -pl benchmark -am -DskipTests
> -Dbasepom.check.skip-all=true` then run `java -jar benchmark/target/benchmarks.jar
> "<class>.<method>" -f 1 -wi 2 -i 4 -prof gc`.

### 2. Config contract redesign
- [ ] Complete the field taxonomy table above.
- [ ] Immutable config snapshot type + `configure(callback)` scoped mutation.
- [ ] Handle config immutable after construction.
- [ ] Move statement-state fields off config/context onto the statement/binding.
- [x] Split defines out of the shared config. `TemplateEngine.render/parse` now take a
      `RenderContext` (config + a per-execution defines overlay) instead of a raw
      `ConfigRegistry`. Defines are no longer forced to live in (and be copied with) the
      heavy config; a template binding's per-execution defines are an overlay applied at
      render time. The immutable-snapshot/`configure(callback)`/handle-immutable pieces
      above are still open.

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
- [ ] **NEXT (planned, green-lit):** Retarget SQL Object onto the template — the full
      breaking refactor. Detailed step-by-step in the HANDOFF "### The plan for next session";
      blocker analysis in "### SQL Object retarget — precise blocker analysis". Introduce
      `Customizable`, retype the customizer SPI + handler base, move `args`/`returner` off
      `SqlObjectStatementConfiguration`, then `SqlQueryHandler` builds one template per method.
- [ ] Reimplement `Handle.createQuery`/`createUpdate`/… on the primitive.
- [ ] Re-run the benchmark for SQL Object, before vs after. Record numbers here.

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
