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
- [ ] Split defines into a dedicated per-render holder (constant + overlay).

### 3. Template primitive
- [ ] Build-time parse + render for the constant-defines fast path.
- [ ] Real `execute()`: fresh binding/context/statement per call, handle-owned
      cleanup, nothing retained on the template between executions.
- [ ] Per-execution defines overlay → re-render path.
- [ ] `reconfigure(callback)` → new template.
- [ ] Encode the thread-confinement boundary in types + javadoc.

### 4. Cache unwinding
- [ ] Memoize resolution on the immutable config instance (eager at build).
- [ ] Remove the config-entangled global caches.
- [ ] Decide whether to keep a single pure `SQL → parsed-fn` cache for classic
      one-shot use; if not, remove it too.

### 5. Unify the APIs
- [ ] Reimplement `Handle.createQuery`/`createUpdate`/… on the primitive.
- [ ] Retarget SQL Object: one template per method, bind + execute per call.
- [ ] Re-run the benchmark for SQL Object, before vs after. Record numbers here.

### 6. Finish
- [ ] Tests: reuse across handles and threads, binding, constant + dynamic defines,
      `reconfigure`, cleanup/resource lifecycle, error paths.
- [ ] Fold this document's content into javadoc + user guide.
- [ ] Delete this file.
