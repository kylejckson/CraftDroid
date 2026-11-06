# Resident implementation validation

Validated in the cloud workspace on 2026-11-06 using Java 21.0.13, Gradle 8.14.3, Minecraft 1.21.1 and NeoForge 21.1.265. The repository remains CraftDroid, forked from MineAgent; internal `mineagent` identifiers preserve compatibility.

## Passed gates

- `./gradlew verifyReleaseArtifacts`: 298 component tests with zero failures/errors, six native Fabric scenarios, four required native NeoForge scenarios, both production JARs, and packaged-class/license checks.
- `python scripts/provision-neoforge-tests.py` followed by the explicitly paid `./gradlew -President_probe=live -Pneoforge_modpack=true :neoforge:verifyResidentGameTests`: seven required native NeoForge scenarios including the live gameplay/social probes with SHA-512-pinned Mekanism 10.7.19.85, AE2 19.2.18 and GuideME 21.1.22.
- The resident native scenario exercises ownerless creation, stable identity/public name through rename and body restoration, inventory retention, actual human block placement/provenance, protected break denial, sharing, no player damage, offhand hazard rejection, cancellation of local work on pause, upper-cell protection for doors, movement rollback, and server-only 192×108 scene capture.
- The real-machine scenario places machines through actual item interactions, inserts a carried raw iron into Mekanism, supplies public FE energy, waits for actual smelting, extracts the resulting ingot, and transfers an AE2 crystal through a charger's public item port. Energy and test supplies are fixture inputs; this is not an autonomous resource-production demonstration.
- A local HTTP fixture verifies the actual DeepSeek-compatible request sends `thinking.type=disabled`, includes PNG image blocks and parses returned tool calls. It does not contact DeepSeek or spend API credits.

## Population measurement

The final combined-mod smoke run created ten persistent ownerless **paused** residents and advanced them for 100 server ticks. The logged heap usage was **315MiB** with a **3,072MiB maximum Java heap**; the test world reported **1,802 loaded chunks**, including the test infrastructure. No model calls occurred. This proves the bodies and selected mods coexist in the test environment. It does not prove ten actively planning residents fit a full 4GB host under real-time pacing, world generation and long-running gameplay. Heap measurements do not include all native memory or OS overhead. The initial 4GB-host operating recommendation remains one or two active residents in a shared pregenerated area.

## Artifact SHA-256

```text
06ef57492dc4c249649a86552fc14f44891ad52cbead65f81ca5054cf28360a9  craftdroid-fabric-1.0.0.jar
4fdb3efe588e60937cfbdb3e4dd129c3c0e3b43fc9191f7b49e6eb02296063a1  craftdroid-neoforge-1.0.0.jar
```

Generated native XML and server logs are preserved by CI. A report must include the named required scenarios and contain no failures, errors or skipped cases; a server that starts without executing tests cannot pass. Ordinary NeoForge regression runs start with a fresh disposable world under `build/`; explicitly resumed town runs retain their isolated checkpoint. Native scenarios require no EULA interaction because NeoForge's development GameTest path automatically accepts the test-server EULA.

## Outstanding validation and limits

Short paid-model crafting/building, spoken server-only vision, two-resident dialogue and relationship-note reload have now passed. See the [live validation and measured results](research/live-residents-2026-11-06.md). A supplied three-resident town has also completed a full 24,000-tick cycle across 20 minutes, with selected real mods loaded; see the [town-life journal](research/town-life-2026-11-06.md). That original cycle had natural mobs and crop growth disabled by GameTest defaults. Multi-day hostile survival, finished town construction and long-term social stability remain unverified. PNG packet delivery and client mixins compile on both loaders, but appearance has not been inspected in an interactive Minecraft client. Server-only vision reconstructs voxel colors and occlusion; it is not a client-quality renderer and excludes entity models and mod textures.

Generic public ports and checked menu interaction are available, but AE2 network autocrafting and all Mekanism production chains are not verified integrations. Pre-existing player builds need OP regions; new normal block placements are tracked. Indirect modded effects, unknown multi-block placement, independent machines and worldgen additions need pack-specific tests. There is no explicit per-resident simulation-ticket cap yet; server simulation distance and separated residents can increase chunk cost.
