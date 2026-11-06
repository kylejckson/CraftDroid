CraftDroid @VERSION@ targets Minecraft 1.21.1 and Java 21.

Forked from [MineAgent](https://github.com/bingdongni/MineAgent).

[Design and usage](https://github.com/kylejckson/CraftDroid/blob/v@VERSION@/docs/research/lifelong-learning-1.0.0.md) · [Historical upstream validation](https://github.com/kylejckson/CraftDroid/blob/v@VERSION@/docs/research/validation-1.0.0.md) · [Changelog](https://github.com/kylejckson/CraftDroid/blob/v@VERSION@/CHANGELOG.md)

## Changes

The learning runtime is exercised against author-published, checksum-locked Extended Drawers 3.0.4+mc.1.21.1, Tech Reborn / RebornCore 5.11.19 and Farmer's Delight Refabricated 1.21.1-3.2.8 on a native Fabric server. Scenarios cover two independent physical learning contexts, exact compensation, a held-out target after an unrelated task and JSON/runtime restart, public energy units, real smelting and heated cooking, cross-mod conservation and a two-agent dependency handoff with exclusive port leases.

A bounded rule archive and task timelines preserve past evidence without restoring stale executor ownership. Rule recall never invents additional support. Plan repair recalls relevant archived operators and selects a backward-relevant subset before applying search limits, while retaining achieved milestones and the owner's acceptance conditions. Unknown consumed inputs cannot be reused symbolically.

The new transfer_port tool moves actual items through public loader interfaces, verifies carried inventory deltas, checks visible faces and interaction permission, and respects owner-scoped resource leases. Learned transfers bind the action to the same observed effect target, dimension, face and actual input, and require an exact reverse compensation. inspect_gui can read a carried item's public acceptance, free capacity and visible menu layout without moving it. Acceptance alone does not identify a recipe role or prove production.

Team jobs have immutable dependency/acceptance contracts and expiring leases. Completion requires the current worker's fresh world observations. Shared rules start as untrusted candidates with zero local supports and no executor; they cannot overwrite contrary local evidence. Reload, memory reset and lifecycle cleanup withdraw obsolete shared state.

Known official OpenAI Chat and Responses models receive explicit breakpoints on declared durable prefixes; volatile world suffixes are excluded. Unknown endpoints retain their existing contract, and stateful incremental Responses chains are preserved. Cache-write tokens, missing usage reports and actual hits remain separate; no paid prewarm calls are added.

Memory envelopes advance to version 15. Old records remain readable, but missing context and changed mod/environment fingerprints require revalidation. The toolchain pins Gradle 8.14.3, Fabric Loom 1.13.6, Fabric Loader 0.20.1 and Fabric API 0.116.17+1.21.1.

## Validation and scope

Release gates run component regressions, five isolated native baseline scenarios, and storage, industry and combined real-mod environments. Both loader artifacts require correct metadata, required production classes, exclusion of test/third-party classes and SHA-256 checksums. Real-mod dependencies remain test-only downloads; they are not redistributed inside CraftDroid.

These are production-component and native-server engineering tests with paused model deliberation. They do not establish autonomous model discovery success rates, arbitrary modpack compatibility, AGI, zero defects or monetary savings. No paid model evaluation was performed. Fabric is supported; NeoForge remains experimental and has not undergone the same real-mod runtime matrix.

## Downloads

- craftdroid-fabric-@VERSION@.jar: Minecraft 1.21.1, Java 21, Fabric Loader 0.20.1 or later and matching Fabric API.
- craftdroid-neoforge-@VERSION@.jar: experimental build for Minecraft 1.21.1 and NeoForge 21.1.x.
- craftdroid-source-@VERSION@.zip and craftdroid-source-@VERSION@.tar.gz: complete tracked source, including wrapper, tests, dependency lock and documentation.
- release-notes.md: these notes plus source commit and CI provenance.
- release-artifacts.sha256: checksums for both JARs, source archives and release notes. CI builds may differ from local artifacts; verify against the manifest accompanying the actual download.

API requests may cost money. Configuration can contain credentials; see SECURITY.md before sharing runtime data.
