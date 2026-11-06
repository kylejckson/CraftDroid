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
