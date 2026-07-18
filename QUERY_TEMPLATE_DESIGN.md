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

## HANDOFF (2026-07-18): sub-step 4 mostly done (17/19 configs immutable); 2 registration facades + D7 remain

**START HERE (clean restart).** Phase 2 (immutable config) is underway. The redesigned target is the
config/Handle decoupling (see that section); its **public API is signed off** in "## Config assembly API"
below (D1–D7 with a sign-off checklist). Where we are: `configure` is now `UnaryOperator`-based; the config
*values* are being made immutable domain-by-domain (D1). The sub-step-3 curated set is done — `Enums`, five
scalar-policy configs, `Arguments` (with a bulk `register(Collection)`), the interceptor trio
(`RowMappers`/`ColumnMappers`/`SqlArrayTypes`, now with `withInferenceInterceptor` + a shared
`RegistrationLists` helper), the shared `MapEntryConfig` (`MapEntryMappers` + vavr `TupleMappers`, whose
registry back-ref was removed), `Extensions`, and `SqlStatements` — and sub-step 4 (below) has now converted
almost all of the rest.

**sub-step 4 — MOSTLY DONE (2026-07-18): 17 of 19 remaining configs converted to immutable.** An earlier note
wrongly claimed "SqlStatements is the last D1 config"; `createCopy()` across all ~34 `JdbiConfig` impls showed
~19 still mutable. Converted this session (each its own whole-reactor-green commit):
- `EnumStrategies` → deleted; became the per-registry `EnumStrategyResolver` (`readAs`), removing a real
  resolution back-ref (it depended on the registry's `Enums`/`Qualifiers`).
- `JdbiCollectors`, `PojoTypes` → immutable (register returns new; resolvers already existed).
- `Qualifiers` → immutable; the `setRegistry` back-ref became a `final` registry via a `(ConfigRegistry)`
  constructor (used only to reach the registry-family's shared, registry-independent qualifier caches).
- Module data configs → immutable: `JsonConfig`, `Gson2Config`, `MoshiConfig`, `Jackson2Config`,
  `Jackson3Config`, `FreemarkerConfig`, `StringTemplates`.
- Behavior/registration configs → immutable: `Handles`, `OnDemandExtensions`, `SqlObjects`, `Handlers`,
  `HandlerDecorators`, `SerializableTransactionRunner.Configuration`.

**TWO configs deliberately NOT converted — `JdbiImmutables` and `PostgresTypes`.** They are registration
*facades* whose `register*` methods write into ANOTHER config (`PojoTypes` / `SqlArrayTypes`) via a per-registry
`setRegistry` reference. A clean immutable conversion needs an invasive resolver-coupling redesign (relocate the
store and re-point `PojoResolver`/array-type resolution, with cross-package visibility friction) that is
disproportionate. Their registry ref is a **registration-boundary** dependency (matching the precedent already
accepted for `Arguments`), and it is used only during `registerImmutable`/`registerCustomType`, never on the
statement read path — so they are not a correctness hazard for a read-through-shared child *until* someone
registers mid-statement (not a real pattern). **Caveat:** `PostgresTypes` is read on the statement path
(`getLobApi` during blob mapping) and is still mutable, so it MUST be made immutable before sub-step-5 zero-copy
is safe on the postgres path. `setRegistry` therefore remains on `JdbiConfig` (these two are its only users).

Remaining, in order: **finish the last 2 configs** (`JdbiImmutables`/`PostgresTypes` registration redesign;
needs a design decision) → **D7** (statement-level copy-on-write private config) → **sub-step 5** (delete the
per-statement `createCopy()`, the perf payoff) → **D4/D5/D6** (builder/plugin/open surface).
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

### Design notes for D7 / sub-step 5 (statement zero-copy) — from this session's analysis
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
  (`createCopy()==this`, no per-registry back-ref). That is exactly why the sub-step-4 config-value
  immutability work is the prerequisite: a value that is mutable or carries a `setRegistry` back-ref cannot be safely read-through
  shared from the parent. The two remaining holdouts (`JdbiImmutables`, `PostgresTypes`) must be finished
  first — and `PostgresTypes` in particular is read on the statement path (`getLobApi`).
- **Sketch of the child** (parent-delegation model): the child holds only its own overrides and delegates
  reads to the parent; the first `install` materialises (or the child forks) so writes never touch the
  parent. Watch two hazards: on-demand config creation must not mutate the shared parent map, and a value
  carrying a back-ref cannot be re-pointed on a shared instance. Both are moot once all values are immutable.

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
Handle h = jdbi.open();                                              // handle references Jdbi's frozen config
Handle h = jdbi.open(cfg -> cfg.with(RowMappers.class, r -> r.withMapper(m)));   // per-handle derived config
```
- **Decision:** drop handle config as a first-class mutable concept. `handle.registerX`/`configure` become
  `@Deprecated` compat shims (derive-and-re-reference), scaffolding only. Keep `jdbi.open(scope)` + scoped
  `withHandle`/`inTransaction` **only if** real usage justifies it; otherwise config is Jdbi-level +
  statement/template-level and the deprecated shims are the sole bridge. Decide when we reach D6 (it is last).

### D7 — Statement / template config: surface unchanged
- `query.registerRowMapper(m).mapTo(X)` stays. Backed by a lazily-created private derived registry
  (copy-on-first-write), frozen at execute. Statements keep `Configurable`. This is what makes sub-step 5
  clean: no additions → the statement shares the handle's immutable config (no copy); additions → exactly one
  derived registry. The per-statement `createCopy()` at `BaseStatement.java:36` goes away.

### Sign-off checklist (SIGNED OFF 2026-07-17)
- [x] **D1** immutable `JdbiConfig`, drop `createCopy`/`setRegistry`, mutators → withers (breaks custom configs).
- [x] **D2** `configure` `Consumer` → `UnaryOperator` — break approved; churn minimized via chainable + plural withers.
- [x] **D3** add `ConfigRegistry.with(Class, UnaryOperator)`.
- [x] **D4** `Jdbi.builder()` primary; `Jdbi.create()` + `registerX`/`installPlugin` deprecated, frozen on first open.
- [x] **D5** add `JdbiPlugin.configure(Jdbi.Builder)`; deprecate `customizeJdbi(Jdbi)`.
- [~] **D6** handle config largely eliminated; `handle.registerX` → deprecated shim; `open(scope)` only if it earns it (decide at D6).
- [x] **D7** statement-level `Configurable` unchanged (copy-on-write private config).

**Implementation order once signed off:** D3 (additive) → D1+D2 per config domain (withers + convenience
re-impl, like sub-step 2's domain-by-domain cadence) → D7 (statement copy-on-write) → sub-step 5 (delete the
per-statement copy, re-benchmark) → D4/D5/D6 (the builder + deprecations + open-scope) last, since they are the
largest public surface and depend on the value/registry immutability being in place.

**Naming (per sign-off):** config-value building methods are prefix-free — `register(...)` keeps its name
(already returns the config type); scalar setters become `templateEngine(e)` / `keyColumn(k)` (no `set`, no
`with`), consistent with `register` and clash-free with `getX()`/`isX()` getters. Apply during D1.

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

### 2. Config contract redesign — NEXT (the retarget surfaced its motivation)
- [x] Complete the field taxonomy table above (reviewed against current sources 2026-07-17;
      added the `extensionState` slot and the post-retarget note).
- [ ] Immutable config snapshot type + `configure(callback)` scoped mutation. **Now motivated
      by the config-mutation footgun:** a customizer that mutates config without the
      `ConfigMutating` marker silently corrupts the shared template snapshot; an immutable
      config turns that into a loud error and lets the late path fork copy-on-write instead of
      falling back to the whole classic path.
- [ ] Handle config immutable after construction (lets the classic fluent path also skip the
      per-statement `createCopy()` — extends the retarget's win beyond SQL Object).
- [ ] Move statement-state fields off config/context onto the statement/binding (per taxonomy).
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
