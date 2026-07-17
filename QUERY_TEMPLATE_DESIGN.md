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
| `extensionState` (opaque, `@Alpha`; added 2026-07-17) | **statement state** (per-execution) — the extension layer's per-invocation holder (SQL Object's args/returner), replacing the deleted `SqlObjectStatementConfiguration` |

Post-retarget (2026-07-17) reality already realized on the template path: per-execution
customizer additions and `queryTimeout` overrides live on the `QueryTemplateBinding` (its
local `customizers` list), not on the shared config; `attributes` are the per-render defines
overlay. What phase 2 still owes: making the *config itself* immutable after build (so the
classic path also skips the copy) and turning an un-marked config-mutating customizer's write
into a loud error instead of the current silent shared-snapshot mutation (see DECISION at top).

## Phase 2 design options (2026-07-17): immutable config — COMPARE & CHOOSE

Grounded in a survey of the actual sources (three research passes). **Landscape:** 40 `JdbiConfig`
impls (35 main, 22 in `core`, 18 public API) — ~21 policy (scalar/flag fields + setters), ~10
registry (append-only `register(...)` collections), **8 that mutate an internal cache on read**
(`RowMappers`, `ColumnMappers`, `Arguments`, `JdbiCollectors`, `Extensions`, `SqlStatements.templateCache`;
`ConfigCaches` is already a shared side-cache, orthogonal). Mutation has **two channels**: A —
`Configurable.configure(Class, callback)` and all convenience methods route through this one seam
(interceptable); B — `getConfig(X).setY()` on the returned object (uninterceptable). **Production
Channel-B per-execution mutation is essentially one case** (config-mutating `SqlStatementCustomizer`s,
already fenced by `ConfigMutating`); the weight of Channel B is **~61 test sites** plus the pervasive,
first-class handle-level mutation (`handle.registerRowMapper/configure/getConfig(X).setY()`, ~98 repo-wide).
The per-statement `createCopy()` at `BaseStatement.java:36` is the safety net that makes every in-flight
mutation correct today; removing it (the perf goal) is exactly what turns Channel-B and the read-caches
into shared-state hazards.

### Two prerequisites shared by ALL approaches (independent of the immutability mechanism)
- **P1 — move per-statement state off config onto the statement/binding.** `define` → per-execution
  overlay; per-execution customizers, `setFetchSize`/`setMaxRows`/timeout → statement-local;
  per-statement `registerX` → a copy-on-write fork. **Already done for `QueryTemplateBinding`** in the
  retarget; the classic `Query`/`Update` need the same when reimplemented on the primitive.
- **P2 — deal with the 8 read-caches.** An immutable config can't self-mutate a lazy cache. Either
  eager-populate at build, or move each to a `ConfigCaches`-style side cache keyed off the (now stable)
  config identity. This couples phase 2 with part of phase 4 (cache unwinding). Unavoidable in every option.

### Common substrate (forced by the findings, not a choice)
Config **values** are immutable; **holders** (`Jdbi`, `Handle`) keep a *swappable reference*.
`handle.registerRowMapper(x)` installs a *new* immutable registry on the handle, so statements created
afterward see it and statements/templates already snapshotted keep theirs. This preserves the
`register/configure` API while letting statements share the handle's snapshot with zero copy. The
extension framework and `BaseStatement` already copy-then-mutate-their-own-copy, so they survive this;
only the per-statement copy is deleted. What no immutable model can keep is `handle.getConfig(X).setY()`
(direct object mutation) — those sites migrate to `configure(...)`.

### Approach A — freeze flag + per-class copy-on-write (smallest delta)
Keep today's `ConfigRegistry` (Class→object map) and typed config objects. Add a `frozen` bit to each
`JdbiConfig` (and the registry); snapshot boundaries freeze it; setters/`register` throw when frozen.
`configure(Class, cb)` on a frozen registry **forks one config class** (copy it, apply, install in a new
registry; other classes shared by reference). Read API (`getConfig(X).getY()`) unchanged.
- **Channel B:** trapped at runtime — a setter on a frozen object throws a clear error (kills the footgun
  loudly). Unfrozen handle/setup config still mutates as today.
- **Blast radius:** touch all ~40 impls to add freeze-checks (mechanical boilerplate); registry gains fork
  logic. No read-site changes. Public API mostly intact.
- **Trade:** least churn and keeps the familiar typed API; but "frozen" is a runtime property, not
  type-enforced — a forgotten check is a silent bug (the same *class* of discipline problem as the
  `ConfigMutating` marker). Cache inconsistency (P2) still must be fixed separately.

### Approach B — immutable value objects; `configure()` returns a new registry (functional)
Every config type becomes an immutable value (final fields, no setters; a wither/builder derives a changed
copy). `ConfigRegistry` is an immutable value over a persistent map; mutation produces a *new* registry
(structural sharing → cheap fork). Holders swap references (the common substrate).
- **Channel B:** eliminated at compile time — no setters exist, `getConfig(X).setY()` won't compile.
- **Blast radius:** rewrite all ~40 impls to immutable form (withers/builders), change `configure` to
  return-new, and **migrate every Channel-B site** (61 tests + customizers + downstream). Breaking public
  API (setters removed). Forces P2 (caches must externalize — values can't self-cache).
- **Trade:** compiler-enforced immutability, no frozen-bit discipline, footgun impossible by construction,
  clean end state; but the largest per-type rewrite and it removes the long-familiar mutable-config-object
  API.

### Approach C — flat typed-key store (the maintainer's idea, refined)
Replace per-type *fields* with a single immutable `Map<ConfigKey<?>, Object>` on the registry. Each config
type declares **typed key constants** (`ConfigKey<TemplateEngine> TEMPLATE_ENGINE` — typed, not a raw enum,
so `get(TEMPLATE_ENGINE)` stays `TemplateEngine`); config types become thin facades over the store or
disappear. Immutability = one immutable map; fork = one persistent-map update (**cheapest possible**).
Registry configs = a key holding an immutable `List`, appended via CoW. Read-caches = separate side store.
- **Channel B:** eliminated by construction — there are no setters; every write is one `put`/one seam.
- **Blast radius:** **largest** — every config field → a key, and **every read site in the codebase**
  changes (`config.get(SqlStatements.class).getTemplateEngine()` → `config.get(SqlStatements.TEMPLATE_ENGINE)`);
  reshapes the `JdbiConfig` SPI (downstream modules/user code that define config types must declare keys).
- **Trade:** cleanest and cheapest end state, immutability + fork in exactly one place, footgun structurally
  impossible, `createCopy` trivial; but the biggest one-time migration (mechanical but wide), and it loses
  the typed config-object grouping unless facades are retained. Registry/cache configs fit a little awkwardly.

### Comparison
| Dimension | A: freeze + CoW | B: immutable values | C: flat typed-key store |
| --- | --- | --- | --- |
| Immutability enforced by | runtime flag (discipline) | compiler (no setters) | compiler (no setters) |
| Channel B (`setY()`) | trapped at runtime | won't compile | doesn't exist |
| Fork cost | copy one config class | persistent-map share | persistent-map share (cheapest) |
| Read-site churn | none | none (read API kept) | **all read sites** |
| Per-type rewrite | freeze plumbing ×40 | full immutable rewrite ×40 | fields→keys ×40 |
| Public API break | small | setters removed | config SPI reshaped |
| Caches (P2) | still ad hoc | forced clean | forced clean |
| End-state cleanliness | lowest | high | highest |
| One-time migration cost | lowest | high | highest |

### End-state verdict (2026-07-17): immutable config-CLASS values (B), not the flat key store (C)

Decision: go directly to the end state, and evaluate flat-keys (C) vs. a richer immutable
config-class interface (B) fairly. On a close look the config-class wins. The reasoning:

**Reframing — immutability, not flatness, delivers the perf goal.** Once config *values* are
immutable, a registry copy shares every value by reference (the per-statement deep copy disappears),
statements share the handle snapshot with zero copy, and a fork allocates almost nothing. B and C give
this *equally*. Forks happen at `configure`/`register` (setup or per-statement registration), never on
the execution hot path, so C's "cheapest fork" advantage is off the hot path and nearly irrelevant.

With perf neutral, the decision turns on reads, cohesion, and migration:

- **Hot-path reads favor B.** The hot path *reads* config. `config.get(SqlStatements.class)` fetches one
  object, then many field reads are free; flat-keys is one map lookup *per field* (`get(TEMPLATE_ENGINE)`,
  `get(PARSER)`, …), and render/execute touch several `SqlStatements` fields together. B amortizes; C
  multiplies lookups (and casts) exactly where it's hottest.
- **Cohesion favors B.** Config classes carry *behavior*, not just data: `SqlStatements.preparedRender`,
  `Mappers/ColumnMappers/RowMappers.findFor`, `Arguments.prepareFor`, `handleException`, `customize`.
  A key store holds data only; that behavior would relocate to free/static helpers that re-pull keys from
  the map — scattering logic and losing the object model the codebase is built on.
- **Migration favors B.** B keeps the read API (`getConfig(X).getY()`) unchanged, so the vast majority of
  config usage (reads, everywhere) is untouched; the change concentrates in the ~40 config classes
  (remove setters, add withers/builder) and the write sites (Channel-B `setY()` → `configure`/wither,
  ~61 mostly-test sites). C additionally rewrites *every read site* and reshapes the SPI — strictly more.

**What C genuinely wins** (and why it doesn't flip the verdict): single-place immutability enforcement
(one map vs. 40 classes to keep correctly immutable) — but **records/compact builders make each config
class immutable with compiler enforcement and little boilerplate**, neutralizing most of this; sparse
footprint (store only non-default keys) — real but modest, and off the hot path; and generic tooling
(serialize/diff/enumerate all config) — which jdbi does not need.

**Hybrid considered and rejected:** config-class *facades* over a flat backing store gives two models and
indirection for no net gain — you pay C's storage plus B's interface. Not worth it.

**Chosen end state (B):**
- Every `JdbiConfig` type is an immutable value (final fields; a `with…`/builder derives a changed copy).
  Prefer records or a compact builder to keep immutability terse and compiler-enforced. Behavior methods
  stay on the class.
- `ConfigRegistry` is an immutable value over a `Map<Class<C>, C>`; because values are immutable it shares
  them by reference on copy (persistent map for cheap forks). No deep copy anywhere.
- `Configurable.configure(Class<C>, UnaryOperator<C>)` derives a new config value and returns a *new*
  registry; the ~30 convenience methods route through it unchanged in spelling.
- Holders (`Jdbi`, `Handle`) hold a swappable reference; `handle.register…/configure…` swap in the new
  registry (future statements see it; existing snapshots are frozen). `getConfig(X).setY()` is removed —
  its ~61 sites migrate to `configure`/withers.
- Prerequisites P1 (statement-state off config) and P2 (externalize the 8 read-caches to `ConfigCaches`-
  style side caches, or eager-populate at build) still apply and are sequenced first.

## Phase 2 implementation plan (2026-07-17): immutable config (end state B)

### Prerequisites (must land before/with the immutability sweep)
- **P1 — statement-state off config.** Done for `QueryTemplateBinding`. Classic `Query`/`Update` need it
  when reimplemented on the primitive; can be sequenced with the remaining phase-5 tail.
- **P2 — externalize the 8 read-caches.** `RowMappers`/`ColumnMappers`/`Arguments`/`JdbiCollectors`/
  `Extensions` (and `SqlStatements.templateCache`) populate an internal `Map` on lookup. An immutable
  value can't self-mutate, so each moves to a `ConfigCaches`-style side cache (already the shared,
  copy-returns-`this` pattern) keyed by the resolution input, or is eager-populated at build.
- **P3 — remove the config→registry back-reference (NEW; found during planning).** ~12 configs override
  `setRegistry` and resolve against the injected `registry` field (e.g. `RowMappers.findFor` →
  `factory.build(type, registry)` / `mapper.init(registry)`). A value shared by reference across a forked
  registry would resolve against the *wrong* (stale) registry, breaking cross-config consistency on fork.
  Fix: **thread the registry through resolution** — `findFor(type, registry)` / resolution takes the
  current `ConfigRegistry` (or `StatementContext`) as a parameter instead of a stored field. This is the
  largest prerequisite and touches the mapper/argument resolution call paths. `setRegistry` is then dropped
  from `JdbiConfig`.

### Contract change
`JdbiConfig<This>`: drop `createCopy()` (immutable values are shared by reference; the registry no longer
deep-copies) and `setRegistry(...)` (P3). It becomes a near-empty marker for "an immutable config value."

### Immutability strategy — records + hand-written services (Immutables considered, set aside)
The maintainer asked to strongly consider the Immutables processor over hand-rolled builders. Evaluated:
the config types are **tiny** (1–4 fields), so none needs a builder at all; and the behavior/registry
configs can't be a generated value type regardless. Immutables shines for many-field pure-value types,
which these are not, and adopting it would mean wiring the annotation processor into core's main compile
(the dep is `provided`/`optional`, not currently an active processor for main; JDK 23+ ignores
classpath-only processors). So:
- **Value/policy configs (~small, no behavior):** Java **records** (native, zero processor wiring,
  compiler-enforced immutability) with 1-line `with…` methods where a scoped change is needed. Covers
  `MapEntryMappers`(2), `ReflectionMappers`(4), `ResultProducers`(1), `TimestampedConfig`(1),
  `StatementExceptions`(2), `Enums`(1), `MapMappers`, `SqlObjects`, and the module configs.
- **Behavior/registry/caching configs:** hand-written immutable classes — final fields, immutable
  collections, `register(...)`/mutators return a *new instance*, behavior methods stay on the class,
  resolution takes the registry as a parameter (P3). Covers `SqlStatements` (render behavior + caches),
  `Arguments`, `ColumnMappers`, `RowMappers`, `JdbiCollectors`, `Extensions`, `Mappers`, `SqlArrayTypes`,
  `PojoTypes`.
- Immutables remains a fallback if we later prefer generated withers; revisit only if a config grows many
  fields. (Records can't be a fallback for the behavior configs anyway — they need methods + non-canonical
  state.)

### Registry + configure + holder
- `ConfigRegistry` becomes immutable over a persistent `Map<Class<C>, C>`; a copy/fork shares values by
  reference and structurally shares the map (cheap). `get(Class)` unchanged for reads.
- `Configurable.configure(Class<C>, UnaryOperator<C>)` derives a new config value and returns a *new*
  registry; the ~30 convenience methods keep their spelling, routed through it. `getConfig(X).setY()` is
  removed — migrate its ~61 (mostly test) sites to `configure`/withers.
- Holders (`Jdbi`, `Handle`) hold a swappable `volatile` reference; `handle.register…/configure…` install
  the new registry. Statements/templates capture the reference at creation (immutable snapshot).

### Suggested landable sub-steps (each self-contained + green)
1. **P3** — thread registry through mapper/argument resolution; drop `setRegistry`. (Non-breaking to the
   immutability model yet; pure refactor, independently testable.)
2. **P2** — move the 8 read-caches to side caches. (Independently testable.)
3. **Contract + registry mechanics** — immutable `ConfigRegistry`, `configure(UnaryOperator)`, holder
   reference-swap on `Jdbi`/`Handle`; keep config objects temporarily mutable behind the new seam to
   isolate the mechanics change.
4. **Convert configs to immutable** (records / hand-written) in waves: policy first, then registry/service;
   migrate `getConfig(X).setY()` sites as each config loses its setters.
5. **Delete the per-statement `createCopy()`** at `BaseStatement.java:36` (the payoff) and re-benchmark the
   classic fluent path; extend P1 to classic `Query`/`Update`.

Risk: this is the largest breaking change in the redesign (~40 config types + resolution APIs + ~61 write
sites). Land it as the sub-steps above, not one commit. Sub-step 1 (P3) is the recommended starting point.

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

- [x] **Rename `buildQueryTemplate` → `buildQuery`?** RESOLVED (2026-07-17): **kept
      `buildQueryTemplate`.** The name matches the returned type `QueryTemplate` (reusable,
      not a one-shot `Query`) and stays distinct from `Handle.createQuery`; least-surprise
      wins. Revisit only when the classic query paths are retired (the remaining phase-5
      item) — then the method and type names can be reconsidered together.
- [x] **Make the `QueryTemplate` constructor non-public.** RESOLVED (2026-07-17): **kept
      public.** Option (a). The SQL Object retarget builds templates directly from a
      method-level config snapshot (cross-module), so a public constructor is the simplest
      seam and mirrors the public `Query(Handle, CharSequence)` constructor. Documented in
      javadoc as an advanced entry point alongside `Jdbi.buildQueryTemplate`.
      Original open text follows for context:
- [ ] **(context) Make the `QueryTemplate` constructor non-public.** Right now it is `public`
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

## DECISION (2026-07-17): customizer phase model — the general fix for config mutation

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

## HANDOFF (2026-07-17): SQL Object retarget DONE — phase 2 (immutable config) is next

**Read this first if picking up fresh.** The SQL Object retarget (phase 5 unify) is **DONE and
committed** (`760d8e8a` non-breaking `Customizable` extraction, `4e0f98d4` the retarget), full
reactor green, sqlobject `sqlobjectSelectOne` ~9.1 → ~4.3 KB/op (~2.1×). See the `## DECISION`
block at the very top for the customizer phase model that made it work.

**The live front is now phase 2 (config contract redesign)** — see "### 2." below. It is
directly motivated by the retarget: config mutation by customizers is uninterceptable today, so
an un-marked config-mutating customizer silently corrupts the shared template snapshot. Making
config immutable-after-build (with `configure(callback)` scoped mutation / copy-on-write) turns
that into a loud error, lets the late path fork one config class instead of the whole classic
path, and extends the allocation win to the classic fluent path. It is breaking (~48
`JdbiConfig` impls) and needs a design pass + maintainer sign-off on the `configure(callback)`
shape before editing. The field taxonomy (task 2.1, above) has had its review pass. Remaining
phase-5 tail item (reimplement `Handle.createQuery`/… on the primitive) can come before or after
phase 2. The historical retarget plan below is kept for provenance.

---

### (historical) phase 5 retarget plan — DONE, kept for provenance

Decision made with the maintainer: **do the full refactor** — breaking the public customizer SPI
is sanctioned for v4.

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
      customizer phase model** (see DECISION at top): configure-phase customizers bake into the template
      snapshot once; bind-phase customizers only touch the per-execution binding; `ConfigMutating` (late)
      customizers fall back to the classic per-statement path. Two latent bugs fixed en route: the binding
      used a separate `Binding` from `ctx.getBinding()` (broke `beforeBinding` customizers), and it skipped
      the `null`-result-set → `NoResultsException` check. `@Timestamped` reworked to a configure-registered
      `StatementCustomizer`. Full reactor green (core 1006, sqlobject 499, all modules).
- [ ] Reimplement `Handle.createQuery`/`createUpdate`/… on the primitive.
- [x] Re-run the benchmark for SQL Object, before vs after (`H2SqlObjectV3Benchmark.sqlobjectSelectOne`,
      `-prof gc`, allocation/op is the deterministic metric in this container):
      | path | alloc/op |
      | --- | --- |
      | classic (baseline) | **~9,100 B/op** |
      | retargeted (template) | **~4,334 B/op** (±176) |
      **~52% less allocation (~2.1×).** Matches the fluent-template win; the ~0.5 KB over the pure
      fluent template (~3.8 KB/op) is the extension dispatch + `ResultReturner` machinery.

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
