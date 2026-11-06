# Autonomous inhabitants: delivery and validation

The target is persistent server inhabitants on NeoForge 1.21.1 using official DeepSeek Flash. Agents operate while their creator is offline, use personal goals and relationships, and obey OP protection zones. Competition excludes PvP and deliberate griefing. Vision must be software-rendered inside the dedicated server.

## Milestones

1. DeepSeek Flash non-thinking controls and bounded multimodal observations.
2. Persistent server inhabitants, OP administration, editable personality and durable social memory.
3. Hard regions, action enforcement, no PvP and property protection.
4. Continuous local execution, autonomous goals, chat/sign perception and shared projects.
5. Server-only scene images and PNG skin delivery.
6. NeoForge native validation, selected mod integrations and resource measurements.

Each delivered milestone is tested before pushing main. This document records implemented behavior and remaining limits; a successful build is not evidence of live paid-model autonomy or ten-agent performance on 4GB.

## Constraints

Keep immutable identities through rename/restart. Bound loaded chunks, perception, planning, requests, images and memory. All world reads/writes remain on the server thread; HTTP and image rendering use background workers. Model plans must use checked gameplay actions and never receive shell, arbitrary Java or OP command execution.

On a 4GB server, begin with one or two agents sharing a pregenerated area. Ten inhabitants, Mekanism, AE2 and exploration remain a measured target. Exact client rendering and arbitrary mod compatibility are not promised.

## Validation status

The component suite (299 tests) and dual-loader release checks pass. See the [dated validation evidence](RESIDENT_VALIDATION.md). Native Fabric scenarios and a real NeoForge dedicated-server scenario exercise resident identity, inventory restoration, protection, no-PvP, and server-only PNG capture. Short live DeepSeek crafting/building, spoken image feedback, two-resident dialogue and durable relationship notes now pass; see the [live validation record](research/live-residents-2026-11-06.md). A three-resident supplied town has completed a full Minecraft day with the selected mods; the [town-life journal](research/town-life-2026-11-06.md) records its substantial remaining gameplay and fixture limits.

## Resident controls (implemented)

`/craftdroid` is OP-only (permission level 2). Use `spawn [name]`, `list`, `inspect <name>`, `rename <name> <newName>`, `pause/resume/remove <name>`, `biography <name> <text>`, `personality <name> <OCEAN trait> <0..1>`, `skin <name> <Minecraft skin name>`, `skinurl <name> <public HTTPS PNG URL>`, `status`, `reload` and `remember <name> <subject UUID> <kind> <text>`. Citizens share a community job board and keep immutable UUIDs across renames and body restoration. Memories are personal attributed notes, including open-ended relationship descriptions; recording a note never establishes the other person's consent or feelings.

Settings are in `config/craftdroid-residents.json`; credentials come from the existing `config/mineagent.json` or `CRAFTDROID_DEEPSEEK_API_KEY` (or `DEEPSEEK_API_KEY`). Defaults use `deepseek-flash`, non-thinking mode, 1,024 output tokens, ten stored inhabitants, two concurrent API requests, 30 request starts per minute across all residents (including tool continuations and retries), and a 60-second idle goal-review interval. Missing credentials pause model activity. Measured live API latency was approximately 1.7–4.0 seconds in the final controlled scenarios; this is not a general performance guarantee.

Resident snapshots are under `<world>/data/craftdroid/residents.json`; memory uses immutable resident UUID directories. Atomic background snapshots are flushed at shutdown; corrupt snapshots block restoration and writes rather than silently discarding history. Citizens restore without a human owner and respawn after ten seconds in Survival with the same identity.

## Hard regions (implemented)

`/craftdroid zone pos1/pos2 [x y z]`, then `zone save <name> <enter,break,place,use>`; `zone list` and `zone remove <name>`. Coordinates are inclusive and dimension-specific; overlapping denials combine. Rules persist under `<world>/data/craftdroid/regions.json`. The model receives the inclusive bounds and denied actions of at most eight nearby regions in current context; all regions remain executor-enforced. Corrupt rules fail closed. Path calculation, live movement, final mining/placement and item interactions are checked. Player combat is blocked at direct attacks and loader damage events, including attributed projectile damage.

Protection is an active development area: pre-existing property needs explicit OP regions. Residents conservatively refuse to mine unknown crafted blocks or containers. Arbitrary modded explosions, autonomous machinery and delayed indirect effects are not yet comprehensively attributed.

Validation: full dual-loader build, six Fabric native scenarios, and a NeoForge dedicated-server scenario passed, including real ownerless creation, rename/restore, inventory retention, final mining denial, direct no-PvP, and server-only scene capture. Release verification rejects missing native scenarios and test-only classes accidentally bundled in production artifacts.


## Local execution, vision and skins

`resident_work` accepts a typed `steps` array of checked physical tool actions; legacy `steps_json` is still supported. Optional semantic conditions have a documented object schema and should only use exact observed fact keys. The local executor advances without a model round trip between steps, stops on failure, and learns a reusable procedure only after verified success. It does not execute arbitrary Java, shell scripts or server commands. Existing navigation, survival reflexes, crafting, recipe discovery and shared work boards remain available.

`inspect_sign` reads the visible side of a nearby sign with exact text. `observe_scene` snapshots a loaded 24-block voxel volume incrementally on the server thread, then renders a 192×108 perspective PNG on a bounded CPU worker. Images are transient model inputs and not permanently duplicated into conversation history. A 30-second per-agent cooldown and global queue bounds limit cost. This is a reconstructed block-color view: it does not reproduce client textures, entity models, sign glyphs or arbitrary mod rendering.

Residents receive deterministic varied procedural skins by default. OP HTTPS PNG links are size/dimension checked, decoded and re-encoded, cached in the world, and sent to matching CraftDroid clients in bounded packets. Supported images are 64×64 or legacy 64×32. This cosmetic client display path is separate from server-only agent vision. PNG appearance has not been visually checked with a human client. Named Minecraft skins remain supported.

Run `./gradlew verifyReleaseArtifacts` for component tests, native scenarios and both loader artifacts. Credentials are never required for these tests. Live model behavior, arbitrary mod machines and sustained ten-agent performance on a 4GB modpack are not established by these checks.

## Community life and property

Use `/craftdroid populate <count>` to create a group with random persistent names, personalities and procedural skins, within the configured population limit. `/craftdroid invite <text>` records a soft project invitation for every resident; include a dimension, town bounds, available supplies and any initial social background in that text. Each inhabitant still chooses goals and collaborators. Seed different personal relationships using `remember` or biography edits; no fixed relationship ladder or forced mutual romance is encoded.

Residents deliberately speak through `resident_say`; ordinary model action preambles remain private. A speech-only call needs no paid acknowledgment. Nearby chat enters personal observations. Addressing a resident by name, or speaking within eight blocks, can wake its model with a ten-second attention cooldown. Resident-to-resident speech wakes explicitly named recipients, limiting conversational fan-out. Ambient perception scans a bounded slice of nearby loaded cells, reads newly observed visible sign text, and notices weather/daylight changes. Older notes about nearby people, queried topics, promises and projects compete with recent notes for the bounded prompt context. `resident_memory` accepts a recall `query`.

Normal new block placements are tracked in a bounded durable property ledger. Residents may maintain community-placed blocks, refuse to break newly placed human property, and refuse unshared containers or machines. Use `/craftdroid share <x y z>` to grant the community access to an existing block; this does not override hard zones. Pre-existing log/stone builds still require OP protection zones because their ownership cannot be inferred reliably. The ledger holds 32,768 occupied cells and fails closed for resident mutations on corruption or capacity exhaustion. Completed vanilla breaks release their entries.

OP pause cancels both paid turns and local procedures, rejects queued body work, and clears movement input. Ordinary physics, hunger and damage continue. Protected movement restores the last permitted body position, or attempts a bounded loaded-area evacuation when a newly created zone encloses an agent; if neither is possible, body scheduling pauses for OP relocation. Protection checks both hands for known incendiary items and checks the extra cells of vanilla beds and doors. Unknown modded multi-block or delayed effects remain outside the current attribution guarantee.

## Selected real-mod validation

The build now targets NeoForge **21.1.265**, still Minecraft **1.21.1**. An isolated optional run pins **Mekanism 10.7.19.85**, **AE2 19.2.18**, and **GuideME 21.1.22** with SHA-512 checksums. Test jars are not bundled in CraftDroid. It exercises normal machine item placement, real FE injection, raw iron insertion, actual Mekanism smelting and ingot extraction, plus an AE2 charger public-inventory transfer using the production platform adapter. Public automation ports remain subject to the action tool's reach, visibility, property and zone checks. This does not establish general AE2 network autocrafting, every Mekanism chemical chain, or arbitrary machine/menu compatibility.

```sh
python scripts/provision-neoforge-tests.py
./gradlew -Pneoforge_modpack=true :neoforge:verifyResidentGameTests
```

CI preserves the native XML and server logs. Each run uses a fresh disposable world under `neoforge/build/`; normal worlds are untouched. Native runs cap the Java heap at 3GB. A ten-resident smoke scenario checks ownerless paused bodies remain present and alive without API spending, and reports heap and loaded chunks. GameTest advances ticks without normal real-time pacing, so its elapsed time is not a TPS benchmark. No explicit per-resident simulation-ticket cap is currently implemented; navigation/perception are loaded-only, but vanilla server view/simulation distances and scattered residents still control much of chunk cost. Begin with a shared pregenerated area and one or two active model-driven residents on a 4GB host.


## Town-life experiments

See the [town-life test journal and actual server snapshots](research/town-life-2026-11-06.md) for a budget-limited discovery run, a successful five-minute continuation, a full 24,000-tick phase, a further ten minutes with crop growth, the fixes found through testing, and the explicit paid commands for starting or resuming an isolated town. These experiments preserve paused residents, inventories, notes and world construction between server processes.
