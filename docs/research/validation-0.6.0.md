# MineAgent v0.6.0 validation record

Historical upstream validation, translated for the CraftDroid fork.

Validation date: 2026-11-03, covering v0.6.0 changes on `codex/v0.6.0-mod-adaptation`. This records local build/test evidence. GitHub Release separately tests, builds and verifies packages. Local checksums identify only these files; use the release's `release-artifacts.sha256` for downloaded attachments.

## Environment and reproduction

| Item | Environment |
| --- | --- |
| System | Windows / PowerShell |
| Java | JDK 21.0.13+9 |
| Gradle Wrapper | 8.10.2 |
| Minecraft | 1.21.1 |
| Fabric | Loader 0.15.11, Fabric API 0.102.0+1.21.1 |
| NeoForge | 21.1.72, experimental loader |
| Model connections | Component tests use local HTTP; native scheduling tests assert zero model calls |

Final command:

```powershell
$env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path
.\gradlew.bat --offline --no-daemon clean verifyReleaseTag prepareReleaseNotes verifyReleaseArtifacts '-Prelease_tag=v0.6.0'
```

`--offline` uses cached dependencies. Omit it for initial Minecraft, loader and Gradle downloads. Linux/macOS use `./gradlew` without PowerShell environment setup.

`verifyReleaseArtifacts` depends on component tests, both loader builds and `verifyGameTests`. Native tests have a five-minute timeout and remove old XML before each launch. Validation requires every specified scenario without failures, errors or skips. CI Build and Release workflows run the same gate and retain reports.

## Component regressions

40 suites, 216 tests, zero failures, errors or skips: the previous 156 plus 60 new tests. Native scenarios are not included in this count.

| Area | Added or strengthened checks |
| --- | --- |
| Conditional learning | Missing/contradictory prerequisites, pre-dispatch changes, argument/condition signatures, sequential composition, repeated physical evidence and distinct output items |
| New actors/items | Type/UUID constraints, runtime ID checks, wrong-type rejection, short-lived instance state, unavailable-field withdrawal and bounded tooltip persistence/replacement |
| Relearning/reflection | One recovery budget for confirmed counterexamples, no refill on execution failure, checkpoint quotas, alternative outcomes, revocation of conflicting rules and interface gaps separate from counterexamples |
| Data reload | No active state, repeated reloads, pending cancellation, late callbacks, cleared predictions, revoked authority, budget reset and clock rollback |
| Planning/execution | Joint target/effect bindings, pre-execution checks, counterexample revocation of preparing skills, repair preserving history/other branches/goals, no invented resources and cross-stage composition |
| Compatibility/recovery | Legacy-context downgrade, new-format round trips, fresh observations after restart and skill revocation on learning reset |
| Cache/usage | Change-only references, republishing after compaction, companion isolation, absent versus zero reports, coverage, malformed metrics without lost answers, overflow and cross-protocol normalization |
| Tool boundaries | Attacks cannot masquerade as compensable experiments; state-changing probes cannot underreport cost; native types cannot be renamed to reset budgets; no recommendations after proposal exhaustion |

Tests use controlled semantic state or local HTTP and validate implementation behavior, not spontaneous model selection of the right next step.

## Real Minecraft server scenarios

`fabric/src/gametest` launches a real headless Minecraft 1.21.1 server through Fabric GameTest. All five scenarios are required:

| Native scenario | Validation |
| --- | --- |
| `learnsTransfersAndStopsAfterRuleChange` | A test-only unfamiliar machine hides trigger rules. Production observations support learning on two devices and transfer to a third. Changed hidden rules revoke the old skill; one recovery allowance learns new outcomes in two independent setups, leaving two new supports and zero budget |
| `observesPublicActorAndRejectsWrongUuid` | Observe a real villager's registered type/UUID without quests/NBT; reject incorrect UUID before dispatch |
| `observesNewItemWithoutDumpingPrivateComponents` | Carry a real test-mod item; verify durability, bounded normal tooltip and truncation markers without arbitrary components |
| `nativeDataReloadInvalidatesLearnedRules` | Call actual `MinecraftServer.reloadResources` through Fabric lifecycle; remove adapted skills and clear predictions; asynchronous waits do not block the server thread |
| `runsObservationsViaTheAgentLoopAndReadsPublicStorage` | Use real `AgentLoop.runMechanismProbe` to read public chest storage; enforce 16-view truncation, no writes, no physical postcondition requirement for reads, immediate insufficient-evidence failure and zero model calls |

Rule-change tests use production learning, `InspectBlockTool`, `SkillRuntime` and native block interactions. The evaluator sets up devices, changes hidden rules and checks outcomes; the learner does not read hidden mappings. The evaluator changes chest quantities to validate read scheduling/projection; a read is not described as causing an increase.

Review fixed actual issues: missing conditional evidence in exploration scoring, missing relearning windows for new outcomes, late competing-rule revocation, mixed output IDs, stale skills after reset and incorrect physical postcondition requirements for read-only `run` probes. The new native scheduling scenario covers the last issue. Inventory fixtures use valid stackable items.

## Artifacts and release checks

- JDK 21 compiled and packaged `mineagent-fabric-0.6.0.jar` and `mineagent-neoforge-0.6.0.jar`.
- Checked existence, loader identity/version, required API/learning/planning/observation classes and licenses; rejected unexpanded placeholders.
- Asserted `LearningGameTests.class` is absent from production JARs.
- Rendered notes, matched `v0.6.0` to `gradle.properties` and generated SHA-256 checksums.
- Existing Java API/Gradle deprecation warnings remain; no claim of zero warnings or Gradle 9 compatibility.

The final clean build succeeded in 2m39s: 25 of 31 actionable tasks executed, six reused cache. This measures one local build, not gameplay responsiveness.

| Artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| `mineagent-fabric-0.6.0.jar` | 1,559,001 | `802bd922fb96d7803b0c090d33db607a676897a37579d144c9ca2c7861f2e176` |
| `mineagent-neoforge-0.6.0.jar` | 1,565,788 | `c41b04357e1f6a261fe48c04f1d81e67fbc8ae36fa349a9f79a6d2db042baf5f` |

Checksums identify this run. Repacked files need newly generated manifests.

The release workflow also creates version-prefixed source ZIP/TAR.GZ archives from the exact tag commit, attaches notes and verifies all five data attachments before upload. Archives contain tracked files only, excluding builds, worlds, configurations and keys. Release text/notes record source commit and CI provenance.

Inspectable evidence:

```text
fabric/build/test-results/test/TEST-*.xml
fabric/build/reports/tests/test/index.html
fabric/build/gametest-results.xml
fabric/build/gametest/logs/latest.log
fabric/build/libs/mineagent-fabric-0.6.0.jar
neoforge/build/libs/mineagent-neoforge-0.6.0.jar
build/release-notes.md
build/release-artifacts.sha256
```

## Conclusions not established

NeoForge passed compilation, packaging and metadata checks; native scenarios ran on Fabric, not equally on both loaders. No held-out external-modpack blind tests, client-only GUI operations, long survival, retention statistics, cross-model comparisons, paid cache/billing experiments or danger-response benchmarks were performed.

Support depends on public observation/legal action channels, not universal compatibility. Reload conservatively invalidates all rules without a precise dependency index. Experience learning/planning proves neither AGI, novelty, superiority nor zero defects. See [continual-learning research](continual-learning.md) for future controlled experiments.
