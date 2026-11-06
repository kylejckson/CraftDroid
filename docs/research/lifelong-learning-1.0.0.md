# v1.0.0: Retaining experience, transferring skills and cooperating in real mods

This release extends v0.6.0's conditional experiments into pinned real Minecraft mod environments. Learning remains external, in experience, skills and plans; it does not train the connected model's weights. Official APIs, relays and local models are supported. The local validation below requires no keys or paid model calls.

## What the implementation adds

The same observation-validated conditional experience determines whether a rule can be recalled, compiled into a skill, used for plan repair or shared as a teammate's learning lead. History, teammate reports and cache hits cannot replace current game observations. The engineering contribution is enforcing these boundaries in execution and regression tests, without claiming a novel algorithm, superiority over related projects or AGI.

| Goal | v1.0.0 changes | Constraints |
| --- | --- | --- |
| Retention of old tasks | Up to 256 active and 1,024 historical rules; favor rare mechanisms, recall by goal and regenerate conditional execution skills | Bounded storage evicts content; memory is not unlimited |
| Long-term memory | Up to 128 tasks with 64 milestones each; preserve starting anchors and recent evidence; separate action success from goal verification | Restored in-progress tasks become interrupted; old executor ownership is not restored |
| Cross-task transfer | Preserve registered type, environment fingerprint, actual inputs, before-state, dimension, port face and effect bindings; observe new targets before executing | Different mod machines are not automatically equivalent |
| Long-horizon planning | Recall relevant history before repair; filter operators backward from goals and prerequisites before applying limits of 64 operators, 128 expansions and eight steps | Model only verified effects; forget potentially consumed inputs rather than creating resources through search |
| Unfamiliar machines | Read actual public ports; inspect carried-item slot acceptance, free capacity and public layout with read-only `inspect_gui` | Acceptance does not identify a recipe role; actual output must be observed |
| Reaction and adaptation | Small public-port transfers synchronously return actual movement and fresh observations; local execution and fast tactics remain independent of the model | Missing public interfaces are coverage gaps, not proof of an incorrect recipe |
| Cooperation | Dependent jobs, 200-tick leases, public-port resource exclusion, fresh-fact acceptance and timeout takeover | Local coordination among one owner's companions, not distributed consensus or arbitrary NPC cooperation |
| Shared learning | Verified teammate rules become candidates with zero local supports/contexts and no executor; two independent local validations are still required | Repeated sharing cannot overwrite counterexamples or inflate confidence; reload withdraws invalid shares |
| Cost | Stable experience references, strict retrieval budgets and durable-prefix cache boundaries for known official new models | Preserve relay/local/other-vendor protocols; no extra prewarming or guaranteed hits/billing gains |

## New game tools

### Public ports and unfamiliar menus

1. `inspect_block` with `capabilities=true` reads visible ports within interaction range. Optional `face` is `up/down/north/south/east/west`; the selected face must actually be visible.
2. `transfer_port` takes `x,y,z,direction,item_id,inventory_slot,amount`. Direction is `into_block` or `from_block`, amount is 1–64, and carried slots are 0–35. Optional `dimension` and `face` bind the observed target.
3. Checks include chunk loading, interaction range, line of sight, world permissions and team leases. Fabric uses public Transfer API transactions; NeoForge uses public item capabilities. Results include `moved_count`, `postcondition_verified` and fresh public observations.
4. If a port is unavailable, open the menu through normal gameplay, then call `inspect_gui({"inventory_slot":0})`. It returns normal menu and player indices, `slot_type`, public `x/y`, `accepts_probe` and `probe_space`, without moving items. Unsupported slot checks are explicitly unavailable.
5. Use `transfer_items` with the observed menu index, then reread input, output and player inventory. Menu indices differ from native inventory indices. An anonymous slot's Java class name does not establish its role.

Port experiments must declare an exact reverse `transfer_port` compensation: identical coordinates, item, amount and carried slot, with the opposite direction. Learning automatically binds the dimension, observed face and carried-input conditions. Declared reversibility does not guarantee restoration through third-party interfaces or after world changes; actual compensation results are recorded separately, including failures.

### Recall and plan recovery

- `explore_mechanism` with `mode=recall, query=...` returns relevant archived rules and budgeted task timelines, read-only.
- `mode=activate, rule_id=...` reactivates verified rules still valid in the current environment and generates conditional adaptation skills. Invalidated, contradicted or old-environment experience gains no execution authority.
- `mode=transfer` still requires observed target type, dimension and conditions. Different port faces do not automatically merge.
- `mode=repair` recalls relevant historical operators before searching. Completed nodes, other branches and final acceptance conditions remain. A proposed plan is not proof of success; execution rechecks prerequisites and effects.

Task timelines render at most 8,000 characters. Default experience references allocate no more than 5,600 characters across rules and task history. Reading history does not increase support counts or rewrite evidence timestamps.

### Team jobs and experience

`coordinate_team` retains `status/commit/request_support/clear` and adds:

| action | Parameters and behavior |
| --- | --- |
| `post_job` | `job_id,objective,resource,dependencies,acceptance`; predecessors must exist; an existing ID's condition list cannot be overwritten |
| `claim_job` | `job_id`; grants a 200-tick lease when all predecessors are verified and worker/resource are free |
| `complete_job` | `job_id`; only the holder can submit; acceptance requires local fresh facts observed no earlier than the claim |
| `share_experience` | `rule_id`; publish only the companion's currently valid rules; shares expire after 24,000 ticks |
| `team_experiences` | Optional `query`; returns up to four relevant teammate history leads |
| `adopt_experience` | `rule_id`; creates a zero-local-support candidate without overwriting existing local experience |

`dependencies` is a JSON array of IDs. `acceptance` is a JSON condition array, optionally string-encoded, with `subject,predicate,value,comparison,minimum_confidence`. Use full observed resource subjects such as `block:minecraft:overworld:10,64,20`. Public-port writes respect that resource's same-owner lease; custom tools must integrate coordination checks themselves. Active execution renews leases; idle leases expire. Death, removal and world shutdown reclaim local shared state. Job completion does not guarantee the resource remains unchanged; successors must observe again.

## Reproducible real-mod environments

The lockfile is [modpack-lock.json](../../fabric/src/modtest/resources/modpack-lock.json). The provisioner downloads only pinned author releases, validates SHA-512 and extracts declared nested dependencies from verified parent JARs while retaining provenance. Mods remain isolated test-runtime dependencies and are not copied into CraftDroid distributions.

| Profile | Pinned real dependencies | Coverage |
| --- | --- | --- |
| storage | Extended Drawers 3.0.4+mc.1.21.1 | Learning on two real drawers with reverse compensation; A→B→A and JSON/runtime recovery; new-target and wrong-face transfer boundaries |
| industry | Tech Reborn / RebornCore 5.11.19 | Rejection by unconfigured ports, normal-menu fallback, actual smelting, real energy capacity and API units |
| combined | Above mods plus Farmer's Delight Refabricated 1.21.1-3.2.8 | Heated cooking, cross-mod item movement, two-companion job dependencies and resource exclusion |

These are selected pinned combinations, not coverage of every public modpack. FD 3.2.8 matches the Minecraft 1.21.1 development toolchain; newer untested releases are not claimed supported. Fabric API 0.116.17+1.21.1 supplies convention tags missing from older development dependencies. Third-party JARs and private rules are not modified to hide problems.

```powershell
py -3 scripts/provision-modpack-tests.py --profile combined
.\gradlew.bat --no-daemon -Pmodpack_profile=combined :fabric:test :fabric:verifyModpackGameTests
```

Switch the profile to `storage` or `industry` for those environments. Worlds, downloaded dependencies and XML reports live under `fabric/build`. Tests pause model deliberation but exercise production observation, item movement, experience learning, adaptation skills and coordination on the real server. They validate engineering behavior, not autonomous model discovery rates.

## Model cache protocol

Continue distinguishing missing cache reports from explicit zeros, retaining read/write tokens and coverage. For official HTTPS OpenAI endpoints and known GPT-5.6 / GPT-6 families, full requests can place explicit breakpoints at declared durable-message boundaries with a 30-minute TTL. Chat and Responses use their respective content-block formats. Dynamic world suffixes have no breakpoints. Incremental Responses chains are not forced into full cached requests. Unknown relays, local endpoints and other models retain their protocols; vendor aliases alone do not trigger private fields.

These policies refer to [OpenAI Prompt caching](https://developers.openai.com/api/docs/guides/prompt-caching). Cache writes may cost money and depend on server capacity/lifetime. This run made no real model calls: it validates request structure and metering, not hit rates or monetary savings.

## Research references and implementation

| Primary source | Inspiration | Implementation and limits |
| --- | --- | --- |
| [Voyager](https://voyager.minedojo.org/) | Accumulation of reusable successful skills | Declarative skills with evidence validity bound to identity, port face, dimension, inputs and counterexamples |
| [A-MEM](https://arxiv.org/abs/2502.12110) | Linked long-term memory and retrieval | Bounded rule archives and task anchors, not its complete link-update algorithm |
| [All-Mem](https://arxiv.org/abs/2603.19595) | Non-destructive evidence and useful information within budgets | Separate historical and active rules; reactivation creates no evidence; preserve negative state so archives cannot revive invalid skills |
| [FlexA-Mem](https://arxiv.org/abs/2610.33274) | Task timelines, exact evidence and bounded rendering | Fixed anchors and milestone retrieval without extra model calls for memory organization; testable rendering budgets |
| [Tech Reborn](https://github.com/TechReborn/TechReborn), [Team Reborn Energy](https://github.com/TechReborn/Energy), [Extended Drawers](https://github.com/MattiDragon/ExtendedDrawers), [FD Refabricated](https://github.com/MehVahdJukaar/FarmersDelightRefabricated) | Real public interfaces, menus and automation restrictions | Original pinned dependencies; agents see normal observations/public ports, not test answers or arbitrary private NBT |

These papers and projects inform the design; their full evaluations were not reproduced, so this project's numbers cannot be directly compared with their tables. See [v1.0.0 validation](validation-1.0.0.md) for measured and unproven capabilities.

## Compatibility and migration

The memory envelope advances to format 15. Existing compatibility logic reads old formats; missing archives/timelines default to empty. Evidence with insufficient condition versions still downgrades; upgrading does not invent missing conditions. Environment fingerprints include mod versions, so upgrading CraftDroid or another mod may require revalidating old skills. Reload invalidates both active and archived rules; recovery still requires current-environment checks.

Minecraft 1.21.1 / Java 21 / Fabric are supported. NeoForge remains experimental: a successful build does not substitute for the same real-mod runtime matrix. Reliability across all mods, public modpacks, multi-day play, autonomous unknown-task completion and arbitrary third-party capability implementations is unproven. AGI is a research direction, not this release's acceptance result.
