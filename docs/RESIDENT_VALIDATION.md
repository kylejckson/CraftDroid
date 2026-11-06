# Resident implementation validation

Validated in the cloud workspace on 2026-11-06 using Java 21.0.13, Gradle 8.14.3, Minecraft 1.21.1 and NeoForge 21.1.265. The repository remains CraftDroid, forked from MineAgent; internal `mineagent` identifiers preserve compatibility.

## Passed gates

- `./gradlew verifyReleaseArtifacts`: 291 component tests with zero failures/errors, six native Fabric scenarios, two required native NeoForge scenarios, both production JARs, and packaged-class/license checks.
- `python scripts/provision-neoforge-tests.py` followed by `./gradlew -Pneoforge_modpack=true :neoforge:verifyResidentGameTests`: three required native NeoForge scenarios with SHA-512-pinned Mekanism 10.7.19.85, AE2 19.2.18 and GuideME 21.1.22.
- The resident native scenario exercises ownerless creation, stable identity/public name through rename and body restoration, inventory retention, actual human block placement/provenance, protected break denial, sharing, no player damage, offhand hazard rejection, cancellation of local work on pause, upper-cell protection for doors, movement rollback, and server-only 192×108 scene capture.
- The real-machine scenario places machines through actual item interactions, inserts a carried raw iron into Mekanism, supplies public FE energy, waits for actual smelting, extracts the resulting ingot, and transfers an AE2 crystal through a charger's public item port. Energy and test supplies are fixture inputs; this is not an autonomous resource-production demonstration.
- A local HTTP fixture verifies the actual DeepSeek-compatible request sends `thinking.type=disabled`, includes PNG image blocks and parses returned tool calls. It does not contact DeepSeek or spend API credits.

## Population measurement

The final combined-mod smoke run created ten persistent ownerless **paused** residents and advanced them for 100 server ticks. The logged heap usage was **489MiB** with a **3,072MiB maximum Java heap**; the test world reported **1,802 loaded chunks**, including the test infrastructure. No model calls occurred. This proves the bodies and selected mods coexist in the test environment. It does not prove ten actively planning residents fit a full 4GB host under real-time pacing, world generation and long-running gameplay. Heap measurements do not include all native memory or OS overhead. The initial 4GB-host operating recommendation remains one or two active residents in a shared pregenerated area.

## Artifact SHA-256

```text
ec5940ca035126b7b52c08af0f45939c06adab9c9512fd8b4250ba2bb9f2a141  craftdroid-fabric-1.0.0.jar
61888724e5329c976837edfbcede8164c744ee879bb597f36149841f43023fba  craftdroid-neoforge-1.0.0.jar
```

Generated native XML and server logs are preserved by CI. A report must include the named required scenarios and contain no failures, errors or skipped cases; a server that starts without executing tests cannot pass. Each NeoForge run starts with a fresh disposable world under `build/`. Native scenarios require no EULA interaction because NeoForge's development GameTest path automatically accepts the test-server EULA.

## Outstanding validation and limits

No live API key was available, so long-running paid-model survival, building, social development, relationship continuity under real dialogue, and model latency/cost have not been demonstrated. PNG packet delivery and client mixins compile on both loaders, but appearance has not been inspected in an interactive Minecraft client. Server-only vision reconstructs voxel colors and occlusion; it is not a client-quality renderer and excludes entity models and mod textures.

Generic public ports and checked menu interaction are available, but AE2 network autocrafting and all Mekanism production chains are not verified integrations. Pre-existing player builds need OP regions; new normal block placements are tracked. Indirect modded effects, unknown multi-block placement, independent machines and worldgen additions need pack-specific tests. There is no explicit per-resident simulation-ticket cap yet; server simulation distance and separated residents can increase chunk cost.
