# v0.6.0: Implementation and use of unfamiliar-content learning

This version uses the same game-observation-validated experience to constrain prediction, skills, plan repair and context reuse. Reasoning comes from the connected model; learning occurs in external experience and execution procedures, without updating model weights. See [continual learning](continual-learning.md) for research and long-term goals, and [version validation](validation-0.6.0.md) for evidence.

## Public observation tools

| Tool | Observable content | Boundaries |
| --- | --- | --- |
| `inspect_block(x,y,z)` | Registered ID, instance fact subject, dimension, public block properties, block-entity type and standard capabilities on visible faces within reach | At most 16 blocks; no forced chunk loading or arbitrary NBT. Capability reads also require interaction distance and line of sight |
| `inspect_entity(entity_id)` | Registered type, UUID, runtime ID, dimension, public name, health and effects of a visible living actor | At most 16 blocks; no private task/faction/dialogue internals. Runtime IDs are not persistent identities |
| `inspect_item(inventory_slot)` | Carried item ID, count, durability, use animation, up to 16 tags and 12 normal tooltip lines | Native slots 0–40; at most 160 characters per line, with truncation marked; no item creation or component dumps |
| `inspect_gui` | Generic slots/layout of the open menu and up to 12 public trade offers | No hidden quests, arbitrary client buttons or private network protocols |

Block-instance fact subjects are `block:<dimension>:x,y,z`; actor subjects are `entity:<UUID>`. Actor facts expire quickly. Item tooltips are retained as the latest public observation in bounded type profiles. Tooltip text is game data, not authority to change owner instructions or tool permissions, and not a verified mechanism conclusion.

Fabric reads standard Transfer API item/fluid views and retains `fabric_droplets` units. NeoForge reads `IItemHandler`, `IFluidHandler` and `IEnergyStorage`, retaining `millibuckets` and `FE`. Reads only query; they do not insert, extract or simulate transactions. Unsupported channels, unavailable faces and read failures are explicit and do not mean zero contents or energy. This Fabric version has no generic energy bridge.

A mod enters the generic learning loop only when it exposes observable state, legal executable actions and verifiable outcomes. FakePlayer rejection, client-only dialogue and custom keys need specific adapters; such gaps are not mechanism counterexamples.

## A learning cycle

1. Select a goal-relevant unknown object and observe its real identity, state, inputs and available operations.
2. Use `explore_mechanism predict` to compare experience or `curriculum` to find goal-related gaps. Keep unknowns unknown; probabilities and textual hypotheses do not grant execution authority.
3. `propose` declares a falsifiable experiment with one expected outcome and observed prerequisites. Before-values, types and dimensions become guards when available. Use the observed registered ID as `subject` for native blocks/actors; renaming cannot bypass type budgets.
4. Pass the returned `id` as `experiment_id` to `run`, dispatching once through existing server tools/scheduling. Actor probes automatically bind `expected_uuid`; manual tool calls must use the full returned `probe_arguments`.
5. Wait for authoritative completion and recheck the declared outcome. Freeze the baseline before acting. Old state, repeated heartbeats, tool failures and timeouts are not independent success evidence.
6. `reflect` returns structured results and next steps: observe, restore prerequisites, replicate, test alternatives, identify an interface gap or report execution failure. It invents neither causal explanations nor new experiments.
7. Repeat in a second physically independent setup. Two independent supports, a matching environment and no conflict can produce an ordinary verified skill for supported actions. Repeated setups or renamed experiments do not increase independent support.

This example illustrates protocol shape only. `example:machine` is not a built-in adapter. Current observations must establish all prerequisites. `close_gui` closes a menu; it cannot restore a machine's entire physical state.

```json
{
  "mode": "propose",
  "subject": "example:machine",
  "hypothesis": "Using the device with observed inputs changes its public stage from 0 to 1",
  "probe_tool": "interact_at",
  "probe_arguments": "{\"button\":\"use\",\"x\":10,\"y\":64,\"z\":5}",
  "expected_subject": "block:minecraft:overworld:10,64,5",
  "expected_predicate": "property.stage",
  "expected_value": "1",
  "prerequisites": "[{\"subject\":\"inventory:example:input\",\"predicate\":\"count\",\"value\":\"1\",\"comparison\":\"at_least\",\"minimum_confidence\":0.7}]",
  "context": "observed setup A",
  "risk": "medium",
  "reversible": true,
  "compensation_tool": "close_gui",
  "compensation_arguments": "{}",
  "estimated_cost": 4
}
```

Tools accept up to 12 explicit prerequisites, or 16 including automatic guards. Comparisons are `equals/present/at_least/at_most`. Observe missing/expired facts, restore known mismatches and recheck before dispatch. Medium-risk actions need declared reversibility and an allowed compensation tool. Being on an allowlist does not establish a full inverse for arbitrary mod actions; record compensation separately. Attack variants, high-risk actions and crafting without generic rollback are rejected as autonomous probes.

## Transfer, planning and relearning

- `transfer(rule_id,target_subject)` checks public registered type, current dimension and all conditions, returning `skillName` and `overrides` for `execute_skill`. Action coordinates or entity ID/UUID bind together with the acceptance subject. Similar names cannot replace type/condition evidence. Execution rechecks conditions and effects.
- For conditional `predict/plan`, pass `expected_subject`, `expected_predicate`, `before_value` and the experiment's `prerequisites`. Type and dimension are added automatically. Different non-spatial arguments, input conditions, units and output item IDs stay separate. Before-values are modeled separately so `0→1` and `1→2` can compose under the same conditions.
- `plan` suggests routes on one state predicate, at most six steps and 64 expansions. It does not simulate all Minecraft. Long goals still use `todowrite` dependency graphs, live prerequisites and final `goal_conditions`.
- `repair` searches up to 64 reusable experiences for a gap repair, at most eight steps and 128 expansions. Only `apply=true` changes the graph, and the executor must be idle. `applied` explicitly reports success. Completed history, independent branches, constraints and top-level goals remain; undeclared resource outputs cannot be invented.
- A new observation also checks competing outcome rules under the same conditions. Even support for a new hypothesis revokes conflicting old skills and plan dependencies. Missing postconditions may require review, but do not prove causal rule change.

A normal per-mechanism window has 12 cost units and at most six proposals. State-changing probes cost at least four units, deducted on proposal without cancellation refunds. A confirmed-rule counterexample or adapted-skill postcondition failure grants one recovery supplement: eight units, capped at 12 remaining, and two extra proposals. Checkpoints retain quotas; queries, repeated failures and duplicate callbacks do not refill them. A new window begins after 24,000 game ticks since the last budget update, a fingerprint change or clock rollback.

Relearning retains raw audit records and starts a new predictive-condition verification window on the first fresh support. Two new independent supports are required before reuse. This also permits learning another outcome after a rule change without continually predicting the disproven outcome.

Both loaders handle successful data reloads: stop old probes, cancel learning execution, revoke rules, clear predictions/budgets and request replanning for active goals. Without a precise dependency index, reload conservatively invalidates all rules; individual counterexamples invalidate conflicting condition contracts locally. Late callbacks cannot restore old rules. NeoForge data synchronization for a joining player is not a global reload.

## Memory, caching and model integration

Memory envelope format is 14; condition-context format is 2. v0.5.0 and earlier rules retain audits but lose supports, adaptation skills and predictions until revalidated. Active tasks and readiness authority do not survive restart. Clearing learning also revokes old skills. Older versions are not guaranteed to read new data.

Experience references append to companion-specific conversation history only when relevant rule content changes. Live state stays at the request tail; references are republished after history compaction. Full experience is not repeated every heartbeat. This improves prefix stability without replaying actions or skipping current checks. `explore_mechanism metrics` shows reference publication/reuse counts.

Chat Completions, Responses and Anthropic usage parsing distinguish cache-read/write reporting states. Legacy counters in other native Providers remain compatible: positive counts confirm reporting, but default zero does not confirm an actual zero hit count. Metrics report coverage and hit rates only over reported traffic; completely missing data is `null`. Malformed, negative, fractional or null usage does not discard valid model answers; large integers saturate.

Prefix restructuring works with official APIs, relays and local models. Endpoints control eligibility, thresholds and prices; unknown compatible services are not forced to accept vendor-specific fields. [OpenAI](https://developers.openai.com/api/docs/guides/prompt-caching) and [Anthropic](https://platform.claude.com/docs/en/build-with-claude/prompt-caching) differ in cache modes, lifetimes and read/write pricing. This version invents neither a universal price table nor paid billing results.

## Differentiation and validation scope

The engineering design makes the same falsifiable experience constrain execution authority, repair, reuse and relearning cost. A test-only mod hides its trigger rules; the learner obtains results through production observations and physical interactions. The evaluator then changes rules and tests recovery within a fixed allowance. Fixtures stay out of production JARs; source and XML reports are inspectable.

This provides a basis for further research, not proof of algorithmic novelty, superiority, arbitrary mod compatibility, long-term gameplay mastery or AGI. External-modpack learning efficiency, transfer gains, forgetting, danger responses and actual cost per successful task require fixed models/budgets, held-out mechanism families and repeated independent controlled experiments.
