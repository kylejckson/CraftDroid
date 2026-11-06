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

Milestone 1: component suite passes with official Flash thinking controls and bounded PNG request serialization. No live provider call has been made. Implementation is in progress. No live provider credentials or paid-model runs are assumed.

## Resident controls (implemented)

`/craftdroid` is OP-only (permission level 2). Use `spawn [name]`, `list`, `inspect <name>`, `rename <name> <newName>`, `pause/resume/remove <name>`, `biography <name> <text>`, `personality <name> <OCEAN trait> <0..1>`, `skin <name> <Minecraft skin name>` and `remember <name> <subject UUID> <kind> <text>`. Citizens share a community job board and keep immutable UUIDs across renames and body restoration. Memories are personal attributed notes, including open-ended relationship descriptions; recording a note never establishes the other person's consent or feelings.

Settings are in `config/craftdroid-residents.json`; credentials come from the existing `config/mineagent.json` or `CRAFTDROID_DEEPSEEK_API_KEY`. Defaults use `deepseek-flash`, non-thinking mode, 1,024 output tokens, ten stored inhabitants, two concurrently admitted goal reviews and a 60-second idle goal-review interval. Missing credentials pause model activity. HTTP/model performance and autonomous success have not yet been measured.

Resident snapshots are under `<world>/data/craftdroid/residents.json`; memory uses immutable resident UUID directories. Atomic background snapshots are flushed at shutdown; corrupt snapshots block restoration and writes rather than silently discarding history. Citizens restore without a human owner and respawn after ten seconds in Survival with the same identity.

## Hard regions (implemented)

`/craftdroid zone pos1/pos2 [x y z]`, then `zone save <name> <enter,break,place,use>`; `zone list` and `zone remove <name>`. Coordinates are inclusive and dimension-specific; overlapping denials combine. Rules persist under `<world>/data/craftdroid/regions.json`. Corrupt rules fail closed. Path calculation, live movement, final mining/placement and item interactions are checked. Player combat is blocked at direct attacks and loader damage events, including attributed projectile damage.

Protection is an active development area: pre-existing property needs explicit OP regions. Residents conservatively refuse to mine unknown crafted blocks or containers. Arbitrary modded explosions, autonomous machinery and delayed indirect effects are not yet comprehensively attributed.

Validation: full dual-loader build and six native Minecraft scenarios passed, including real ownerless creation, rename/restore, inventory retention, final mining denial and direct no-PvP. The native scenarios currently run on Fabric; dedicated NeoForge runtime validation is still outstanding.
