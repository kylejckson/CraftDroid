# Continual learning, mechanism transfer and cost validation

This document describes the development branch's design, research basis and validation limits. With the base model and tool permissions fixed, the goal is for gameplay experience to improve later predictions, exploration choices and skill reuse. It claims neither AGI nor superior live performance over external projects.

The feature version described here is **v0.6.0**. It adds unfamiliar-object observations, multi-condition experience, structured reflection, type-constrained transfer, bounded relearning and experience-based repair. See [v0.6.0 design and tools](mod-learning-0.6.0.md) and [validation](validation-0.6.0.md). Versions 0.x remain initial development; names do not replace quality evidence. See [SemVer](https://semver.org/).

## Research basis and project design

| Source | Relevant finding or mechanism | CraftDroid's approach |
| --- | --- | --- |
| [Voyager](https://github.com/MineDojo/Voyager) | Autonomous curricula, executable skills and environmental feedback support continual learning | Retain the skill runtime; add curricula from observed knowledge gaps and target bindings |
| [Active World Model Learning with Progress Curiosity, ICML 2020](https://proceedings.mlr.press/v119/kim20e.html) | Learning progress distinguishes learnable dynamics from noise better than persistent prediction error | Measure prediction-loss changes on verified experiments; reduce priority for stable known mechanisms and noise without progress |
| [DreamerV3 implementation / Nature 2025](https://github.com/danijar/dreamerv3) | Predictive world dynamics provide a shared basis for learning and planning | A small symbolic conditional predictor, without reproducing its neural world model, reinforcement-learning training or scores |
| [WISE, arXiv v2](https://arxiv.org/html/2606.12852v2) | Semantic affordance memory and opportunity scheduling connect observations to task relevance | Goal-filtered observed mechanisms; predictions expose outcomes and uncertainty |
| [PEAM, Findings of EMNLP 2026](https://aclanthology.org/2026.findings-emnlp.1326/) | Internalizing experience into fast execution modules is a research direction | External evidence and executable skills; parameter training and LoRA distillation are not implemented |
| [PSN, arXiv v2, 2026](https://arxiv.org/abs/2601.03509) / [implementation](https://github.com/evolving-skill-networks/psn) | Compositional skill networks, structured fault localization, maturity gates and rollback verification already exist | Do not claim composable skills alone as novel; focus on experiment results calibrating prediction, exploration and transfer acceptance |
| [Agentic Harness Engineering, arXiv v4, 2026](https://arxiv.org/abs/2604.25850v4) | Connect observed experience and expected outcomes through falsifiable contracts | The same conditional experience constrains skills, repair and reuse; no automatic harness rewriting or reproduced scores |
| [Anthropic long-running agent harnesses](https://www.anthropic.com/research/effective-harnesses-for-long-running-agents) | Explicit progress artifacts and real validation support work across contexts | Persist raw experiments/conditions; recompute execution authority on restore; report real-server tests separately from component regressions |
| [JARVIS-1 implementation](https://github.com/CraftJarvis/JARVIS-1) | Multimodal experience memory assists planning and hierarchical control | Memory/planning already have strong baselines; add inspectable conditional outcomes and counterexamples, without cross-project performance claims |
| [Optimus-3, arXiv v2](https://arxiv.org/abs/2506.10357) | Task/hierarchical routing coordinates fast execution with deep reasoning | Remove built-in companion request serialization while retaining local execution; no reproduction of MoE, training data or DGRPO |
| [Mindcraft implementation](https://github.com/mindcraft-bots/mindcraft) | A mature open-source Mineflayer/LLM reference | Multi-model connectivity alone is not distinctive; server components permit direct outcome validation, but differ from visual-control baselines |
| [DEPS, NeurIPS 2023](https://arxiv.org/abs/2302.01560) | Long Minecraft plans need execution feedback, failure explanations and subgoal selection | Server facts check prerequisites; schedule by priority and remaining dependency depth, without reproducing its trainable selector or scores |
| [AdaPlanner, NeurIPS 2023](https://proceedings.neurips.cc/paper_files/paper/2023/hash/b5c8c1c117618267944b2617add0a766-Abstract.html) | Feedback refines plans through in-plan corrections or reconstruction | Separate unknown evidence from contradiction; preserve completed history and repair suffixes; feedback planning itself is not claimed novel |
| [MirrorCraft, arXiv v2, 2026](https://arxiv.org/abs/2607.29218v2) | Paired worlds with matched terrain/resources/goals/interfaces/budgets isolate hidden-rule changes | A basis for future live benchmarks, not a reproduced evaluation or a claim that rule-change testing is unique |
| [MC-EvoBench, arXiv, 2026](https://arxiv.org/abs/2610.13829) | Separate durable transfer from carried resources, source-task exposure and prerequisite bottlenecks | Future experiments reset target worlds, permit only declared experience artifacts and compare relevant/irrelevant/shuffled histories; benchmark not yet run |

The design contribution combines falsifiable conditional experience across prediction, exploration, action/acceptance bindings, plan repair, experience-context reuse and relearning budgets. Counterexamples change execution authority and pending dependencies; invalidation updates stable model references rather than merely appending reflection text. This is an inspectable engineering combination. Establishing a new research algorithm requires controlled experiments and broader literature comparisons.

## Differentiation and research claims

The proposed positioning is an AI player that forms, tests and transfers experience under unfamiliar rules. Existing work already covers skills, memory, reflection, curricula and fast/slow reasoning. Combining those modules or claiming autonomous completion does not establish differentiation. The testable hypothesis is that, with a fixed base model, interaction experience reduces trial and error on unseen layouts, rule changes and composite goals while lowering total cost per successful task.

| Capability | Current progress | Missing evidence or capabilities |
| --- | --- | --- |
| Learning and memory | Verified interventions form persisted conditional predictions; renaming a physical setup cannot increase support | Long-term old-task retention, evidence selection and multimodal state representation |
| Planning and innovation | Independently learned transitions form new short routes; long DAGs check live prerequisites, choose critical chains and repair suffixes | Automatic long plans across multiple objects/predicates; novel hypotheses still depend mainly on the connected model |
| Reaction and adaptation | Concurrent built-in companion sessions; separate missing evidence, cancellation and failure; contradicted routes stop being recommended | P50/P95 danger-response ticks and server time; learned fast policies |
| Exploration and unknown problems | Predictions, progress and goal-related knowledge gaps guide budgeted probes | Controlled interventions excluding external interference, long-horizon active exploration and hidden-mechanism blind tests |
| Cost and caching | Stable Anthropic blocks, cache read/write metrics and local skill execution | Actual official/relay/local bills, eligibility and cost normalized by successful tasks |

v0.6.0 adds multi-predicate conditions, type-constrained instance transfer and native server scenarios with test-only unfamiliar machines. Next, broaden mechanism families and external mods, test cross-object relationships and retention, then evaluate distilling high-confidence skills into lightweight local policies. Automatic rewriting/training should follow frozen old policies, independent held-out tasks and regression checks. No autonomous weight updates are deployed.

## Unfamiliar-mod learning and transfer: long-term goals and acceptance

Added 2026-11-03. The requested AI player should enter mods/modpacks without preloaded guides, use available in-game information and feedback to learn actors, rules, environments, gameplay and items, reflect on failure, transfer experience and combine new content. This constrains learning, long planning, reaction, innovation, memory, exploration, adaptation and cost per successful task. These remain full research goals: v0.6.0 closes the engineering loop on supported public interfaces, without establishing complete cross-mod mastery.

### Tool coverage and compatibility boundaries

`LookAroundTool` recognizes generic containers and some facilities; other block entities generally expose `inspect/interact`, which does not explain machine rules. `InspectGuiTool` reads generic open-menu slots but does not generically handle custom buttons, quest dialogue, energy bars or client-only screens. `LookupRecipeTool` queries what Minecraft's recipe manager and generic recipe interfaces express, not arbitrary private recipes, unlocks or specialized processing. `AdaptationRuntime` compiles confirmed rules only for its limited supported action tools.

Evaluate three conditions separately: can object state be observed, can required actions execute, and does experience improve later behavior? Missing interaction is a coverage gap, not proof a mechanism is unlearnable. NPC FakePlayer rejection, client-only interactions, dedicated keys and custom protocols need separate compatibility validation.

Within compatible Minecraft/loader versions, prioritize reusable observation/action bridges, then small adapters where generic interfaces cannot express content. Adapters declare observation fields, units, permissions and side effects, exposing agreed observations and legal operations. Hidden-rule discovery tests must not provide rule answers, infer them from private implementation source or bypass in-game unlock/interaction conditions.

[NeoForge 1.21/1.21.1 capabilities](https://docs.neoforged.net/docs/1.21.1/inventories/capabilities/) provide generic item, fluid and energy interfaces. Individual mods must expose them. An available capability neither teaches machine purpose/causal conditions nor authorizes all server-state reads.

### What must be learned

| Content | Required experience | Example acceptance |
| --- | --- | --- |
| New actors and creatures | Interaction, behavior, affiliation, dialogue/quest prerequisites; distinguish observation from guesses | Interact correctly with another instance; update strategy when a neutral actor changes state |
| New items and devices | Properties, use conditions, inputs/outputs, slots and operation order; retrieve public descriptions as needed | Demonstrate supported interactions and actual output for unfamiliar materials/devices |
| New rules and gameplay | Multi-condition/timing mechanisms, unlocks, resource cycles, risks and failure causes | Distinguish missing energy/materials, incorrect operations, incomplete waits and changed rules |
| New environments | Relations among dimension, biome, time, weather, hazards and outcomes | Preserve generic survival while learning environment-specific constraints |
| Cross-object/mod combinations | Transferable relations abstracted from names/coordinates, with consistent inputs/outputs, units and prerequisites | Feed one mod's output into another's processing chain and verify the final goal |

All transfer checks applicability. Names, appearances or identical IDs suggest analogies but cannot authorize reuse alone. Similar-purpose energy, fluid and recipe types may still be incompatible.

### Shared evidence constraints on experience, planning and reuse

Structured experience should connect prediction, skills, plans and decision reuse, recording objects/relations, actions, prerequisites, observations, independent support, counterexamples, environment scope and dependent skills/plans. Interaction acceptance does not mean the gameplay goal is complete.

The target loop discovers unfamiliar objects and capability gaps, retrieves in-game descriptions/unlocked information/observations, proposes distinguishable hypotheses, performs small budgeted experiments, waits for and checks effects, compiles rules into skills/plans, and retests on new instances/combinations. Reflection must change inspectable conditions, hypotheses or the next experiment rather than append an explanation.

Verified contradictions pause dependent automatic skills and decision reuse and repair pending plans, while preserving unrelated experience and completed history. Record missing observations, timeouts, tool faults, external concurrent interference and genuine counterexamples separately. Use controls or repeated experiments to exclude interference when possible; sequence alone is not causality.

Fingerprints cover game version, loader, mod IDs/versions and some registry IDs, not proof that configurations, datapacks, scripted recipes or world progression match. v0.6.0 handles successful data reloads by conservatively invalidating all rules when dependencies are unknown; individual counterexamples invalidate conflicting contracts locally. Finer rule provenance/dependency indexes should reduce needless revalidation. Matching mod lists alone cannot establish rule validity.

Schedule local reaction separately from model planning. Execute verified skills only under current conditions; danger, changed prerequisites or conflicting evidence pause/escalate reasoning. Late model suggestions still check current goals and conditions. Official APIs, relays and local models can improve through external experience/skills without requiring weight training.

### Mod knowledge and cache cost

Do not inject entire registries, guides and histories every round. Retrieve relevant objects, rules, skills and tools by goal. Keep an execution segment's tool/knowledge snapshot stable and append live changes afterward. Actor state, inventory, progress and machine conditions must remain fresh; do not hide changes to improve cache hits.

Separately record server cache reads/writes, model calls avoided through local skills, and retry/experiment/retrieval/summary overhead. Mark endpoints without usage/prices unobservable rather than treating missing fields as measured zero cost. Decision reuse checks world/task scope, rule/skill versions and prerequisites, stopping dependent reuse on rule changes. This differs from vendor prefix caching.

Primary cost is all attempts' cost divided by successful tasks on a fixed task set, alongside success rate, timeouts and failure cost. Record local inference time/resource use when available and use actual endpoint prices or labeled estimates. If all attempts fail, report no successes and total expenditure without inventing unit-success cost. Uniform hit rates or savings across endpoints cannot be promised.

### v0.6.0 and v1.0.0 mod acceptance goals

| Version | Required delivery | Acceptance scope |
| --- | --- | --- |
| v0.6.0 | Generic observation/action coverage diagnostics; conditional experience and targeted experiments; supported-mechanism plan repair; versioned reuse and cost records | Representative declared real mods and controlled hidden mechanisms: actors/items, layout transfer, new combinations, rule changes and post-restart reuse |
| v1.0.0 | Sustained multi-stage gameplay within a support matrix; cross-task/mod composition; local counterexample-driven relearning; retention; stable interfaces/save migration | Repeated independent long runs, multiple modpacks/configurations, resource contention, interruption recovery and endpoint changes; new content requires revalidation |

Report interface compatibility separately from autonomous learning; also separate public-description-assisted play from pure hidden-rule discovery. Adapter coverage, public-recipe success and unknown-rule learning gains cannot be combined into one generalization score.

Core protocol: learn under rules A, reset target-world resources/progress, carry only declared experience into B, change layout/instances/composite goals, modify one rule to measure relearning, then return to A to measure retention. Split development and held-out evaluations by mechanism family. Fix model, observation/action permissions, environment steps and model-call/token budget; report independent repeated runs and confidence intervals.

Compare no history, relevant, irrelevant and shuffled experience. Ablate prediction, relational transfer, skill reuse and selective invalidation separately to distinguish transfer from model priors, carried resources and extra compute. Diagnose observation/action coverage at each stage so early interface failures do not leave learning untested.

Measure effective interactions to first learning, held-out success, transfer gains, recovery interactions, old-task retention, danger-to-perception/response delay, server time, model calls, observable cache reads/writes and total cost per successful task. Count new gameplay combinations only when game-state acceptance verifies them; repeated attempts, novel wording or skill-file counts are not capability/innovation measures.

## Implemented loop

1. Freeze the pre-action baseline, then execute a budgeted, compensation-constrained probe through the scheduler.
2. Admit only newer, correlated support/counterexamples. Execution failures, timeouts, cancellation and unchanged state are not success evidence.
3. Identify independent contexts from actual targets, normalized arguments and before-state. Caller/model context names are annotations; renaming cannot increase support.
4. `PredictiveLearning` separates mechanism, tool, non-spatial parameters, predicate, before-value, fingerprint and condition signature. Top-level `x/y/z` can abstract; type-guarded actors can abstract runtime ID/UUID to target actors. Faces, slots, amounts, output item IDs, units, dimensions and nested configuration remain exact. Before-values are constrained by the prediction key, not duplicated in condition scope, allowing sequential transitions.
5. Outcome distributions use categorical counts and 0.5 pseudocounts, retaining probability for unseen outcomes. Expected single-step entropy reduction and recent prediction-loss improvement are exploration proxies, not full causal information gain. With no experience, all mass is unresolved; zero entropy/information fields mean unestimated, not certain. Only then may recommendation use caller cold-start estimates, without inventing binary outcomes.
6. Predict from the latest 16 independent contexts per condition, retaining at most 256 historical records. Contradictions raise uncertainty so old evidence cannot suppress changed rules forever.
7. `AdaptationRuntime` compiles confirmed rules into ordinary skills with named bindings updating action coordinates and effect targets together. Execution still uses `SkillRuntime`, survival priorities and server verification.
8. Persist predictive evidence with mechanism memory. Format 14 adds conditional experience/recovery quotas. Missing fields use safe defaults; other fingerprints do not inform current predictions; restoration grants no old physical-task ownership/readiness. Context format is 2. Importing v0.5.0 format 1 or older caller-named contexts retains audits, clears predictions, downgrades confirmed rules to zero-support candidates, removes adapted skills and requires new independent support.
9. Compose reliable transitions into routes of at most six steps/64 expansions, excluding single-observation and review-pending transitions. Rank by steps/probability and suggest without executing. Scope is one observable predicate of one mechanism, not a full-world simulation.

## Long plans: achievement history versus current executability

`PlanGraph` is separate from six-step experience search. A declaration permits 64 dependency nodes; prompts roll over four nodes. Ready nodes sort by explicit priority, then longer remaining chains, then declaration order. Chain length assumes unit step duration and is not a gameplay-time prediction.

`todowrite.todos[]` adds `prerequisites` using the same fields as `goal_conditions`:

```json
{
  "id": "craft",
  "content": "Craft the target item from available materials",
  "priority": "medium",
  "status": "pending",
  "depends_on": ["gather"],
  "prerequisites": [
    {"subject": "inventory:mod:input", "predicate": "count", "value": "3", "comparison": "at_least", "minimum_confidence": 0.7}
  ]
}
```

- Completed `gather` remains history. Later material loss blocks dispatch of `craft`; replenish or repair the graph suffix.
- Missing, stale or low-confidence facts emit `prerequisite_unknown`; fresh mismatches emit `prerequisite_changed`. Missing observations are not causal counterexamples.
- Complete inventory scans publish zero for explicitly tracked absent items, not unobserved external containers, machines or actors.
- Accepted actions may consume inputs. Falling prerequisites neither cancel admitted execution nor erase completion. Unadmitted model-reported `in_progress` cannot bypass checks.
- Repeated identical blocks do not wake the model every tick or on unrelated updates. Restoring and losing a prerequisite retriggers; other independent ready branches can run first.
- Existing server scheduling retains local execution/survival control. Prerequisites constrain model-dispatched physical actions; observation/repair tools remain usable.

v0.6.0's `ExperienceRepairPlanner` searches at most 64 confirmed operators, eight steps and 128 expansions. It simulates only declared effects, forgets possibly consumed inputs and preserves identity guards. Each executed step still needs observation/acceptance. It does not guarantee globally minimal repair or compile arbitrary natural-language success into predicates. Long tasks should declare `goal_conditions`; top-level observations determine success.

Predictions answer conditional experience questions. No data means no empirical prediction; a prediction neither authorizes actions nor proves sole causation. External concurrent events, unmodeled conditions and incorrect mechanism identity remain live-validation limitations.

## Tool entry points

`explore_mechanism` adds:

- `predict`: `subject`, `probe_tool`, `probe_arguments`, `expected_predicate`, `before_value`; returns independent contexts, outcome probabilities, uncertainty, information value and review flags.
- `plan`: mechanism, predicate, current/goal states; compose verified transitions, optionally bounded by `max_steps`. Observe missing current state first; contradicted transitions leave new routes.
- `curriculum`: bounded candidates from observed gaps, optionally goal-filtered by `query`; no physical dispatch or additional model requests.
- `metrics`: model calls, inputs, cache reads/writes, outputs and terminal action outcomes.

`propose/recommend/status/abort` remain compatible. `recommend` uses empirical value when available, otherwise caller estimates as cold-start priors.

`list_learned_skills` returns default `bindings` for transferable adaptation skills:

```json
{
  "skill_name": "name returned by the skill query",
  "overrides_json": "{\"bindings\":{\"target_x\":99,\"target_y\":64,\"target_z\":5,\"effect_subject\":\"machine:new\"}}"
}
```

Override only declared parameters; some probes expose only `x`, not `y/z`. Existing perception/planning still handles discovery, target selection and transfer suitability. Legacy per-step overrides work. Named bindings retain JSON types; missing, cyclic or mistyped bindings reject startup.

## Response speed and caching

Built-in Providers create independent transports through `newSession()` per companion. HTTP clients may be shared; Responses sessions/endpoint fallback state are companion-owned. Existing locks preserve legacy extension behavior; Providers without the new interface remain synchronous. Registered instances no longer serialize all built-in companion HTTP traffic.

Anthropic separates the first stable system instruction from rolling summaries and keeps the breakpoint on the stable block. Summary changes do not rewrite that block; tool changes still affect the prefix.

`cacheableMessageCount` declares the durable conversation boundary, with live state afterward. Anthropic adds a second breakpoint there; even if tool results merge with later live user text, it remains on the tool-result block. Legacy callers without a boundary receive no extra breakpoint. This is not a universal parameter sent to arbitrary compatible/local endpoints.

[Anthropic caching](https://platform.claude.com/docs/en/build-with-claude/prompt-caching) distinguishes breakpoints, history growth, lookback, token minimums and writes. Local HTTP validates request structure, not server hit rates/billing. [OpenAI Prompt Caching](https://developers.openai.com/api/docs/guides/prompt-caching) depends on matching prefixes/settings and may charge for writes. The branch reads `cache_write_tokens` and records reads/writes separately without forcing new controls on unknown relays/local endpoints.

`previous_response_id` maintains conversation continuity; [historical inputs remain billable](https://developers.openai.com/api/docs/guides/conversation-state). v0.6.0 separates missing reports from explicit zero, uses reported inputs for observable hit-rate denominators and shows coverage; entirely absent data is `null`. Stable experience references append only on change; live state remains last. Measure actual hits/prices/savings on the target endpoint.

Count tool queries, async dispatch, success, failure, cancellation and insufficient evidence separately. Duplicate callbacks do not double-count; pending work is not failure. Failed model requests still contribute actual waiting time.

## Validation and next live experiments

See [v0.6.0 validation](validation-0.6.0.md), with historical comparisons in [v0.5.0](validation-0.5.0.md), the [2026-11-02 baseline](validation-2026-11-02.md) (136 regressions) and [v0.5.0-alpha.1](validation-0.5.0-alpha.1.md) (156 regressions).

Regressions cover location transfer, parameter/before-state/environment isolation, repeated evidence, drift, JSON checkpoints, invalid bindings, legacy overrides, transferred-target acceptance, session isolation, concurrent local HTTP, stable Anthropic cache blocks, cache-write metrics and idempotent terminal outcomes.

Component tests use production code with controlled semantic state/local HTTP. v0.6.0 also runs a test-only unfamiliar mod, native actor/item observations and real data reloads on Minecraft servers. These do not replace external-modpack blind tests or long survival.

Proposed live experiment: give a companion an unseen processing device without hidden answers, allow observation/experiments/skills, then change location/layout and one rule to test transfer, contradiction detection and relearning. Evaluators may inspect full state; agents receive only agreed observations. Count public-recipe lookup separately from hidden-rule discovery.

Fix model, seed, permissions and budgets; independently disable prediction, binding transfer and progress scoring. Report repeated-run completion, learning interventions, transfer, recovery, retention, danger-response ticks, server time and cost per success. Publish no percentage gains or savings before running these experiments.
