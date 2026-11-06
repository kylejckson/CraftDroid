# v0.5.0-alpha.1 development build validation

Validation date: 2026-11-02 (Beijing time). The feature target is v0.5.0. This record describes the local `codex/adaptive-learning-runtime` workspace, developed from `701250a950154667212d3ae1be3b3197e9ce9470`, before a new remote release. See the [continual-learning design](continual-learning.md) for research, tool usage and limitations.

## Final results

- Windows, Temurin JDK / javac 21.0.13+9, Gradle Wrapper 8.10.2.
- Fabric and NeoForge compiled and packaged successfully from clean output directories.
- Shared-component tests under Fabric: 33 suites, 156 tests, zero failures, errors or skips. This adds 20 tests over the previous run; an input-boundary test also covers 12 real tool entry cases.
- NeoForge has no independent test sources. Its production compilation, packaging and artifacts were checked, not complete gameplay.
- `git diff --check` passed.

Final command:

```powershell
.\gradlew.bat --no-daemon clean verifyReleaseTag prepareReleaseNotes verifyReleaseArtifacts '-Prelease_tag=v0.5.0-alpha.1'
```

The dependencies of `verifyReleaseArtifacts` run both loaders' `build` tasks, including Fabric tests.

## Added coverage

| Area | Checks performed |
| --- | --- |
| Long-horizon prerequisites | Distinguish unknown, contradicted and satisfied conditions; a model's self-reported running state cannot bypass them; material consumption does not erase accepted execution or completed history |
| Suffix repair | Preserve completed nodes, add material-replenishment nodes and resume after restoring dependencies; independent ready branches can execute first |
| Scheduling and persistence | Explicit priorities; unit-duration critical dependency paths; expired observations; checkpoints do not restore old readiness authority; new targets cannot borrow old evidence; blank IDs cannot split bindings |
| Controlled 32-step scenario | Import a checkpoint after step 9; lose resources before step 17; 50 identical blocked checks produce no duplicate repair signals; restoring materials allows completion, with each step executing once |
| Facts and parameters | Complete inventory observations provide zero counts for tracked missing items; unknown external objects are not assumed absent; invalid or non-finite numbers return errors instead of null-unboxing exceptions |
| Cache protocol | Explicit durable-history boundaries; Anthropic breakpoints stop at tool results, excluding merged live user blocks; legacy callers do not enable extra breakpoints by default |

Controlled scenarios use production planners and semantic-state interfaces, not autonomous model-generated Minecraft completion. HTTP tests connect only to local test servers, not paid model endpoints.

## Release consistency checks

- The version in `gradle.properties`, tag argument, generated release notes, exact JAR names and embedded loader versions agree.
- Passing `v0.4.0` to `verifyReleaseTag` fails as expected and requires `v0.5.0-alpha.1`. No Git tags were created or changed.
- Generated notes contain neither `@VERSION@` nor stale `0.3.6` copy.
- Both JARs contain the API Provider interface, experience predictor, long-horizon planner, skill binder, license and corresponding loader metadata.
- GitHub workflows use the same validation tasks and select checked version-specific upload files. The workflow itself was not run on GitHub Actions in this local validation.

| Artifact | SHA-256 |
| --- | --- |
| `fabric/build/libs/mineagent-fabric-0.5.0-alpha.1.jar` | `1b256cc47187eea58b08d4d376ca17327fc5f44c57c10630259ba46c9ad40cee` |
| `neoforge/build/libs/mineagent-neoforge-0.5.0-alpha.1.jar` | `44355b7e990bfd0445a3361b7fa3e1f79469d932afccfc9265cfac09278ee834` |

The generated checksum manifest is `build/release-artifacts.sha256`; release notes are `build/release-notes.md`. These hashes identify this local build and do not promise identical bytes across platforms.

## Validation limits

No long-duration Minecraft runtime, unfamiliar-mod blind test, fair cross-project comparison, visual control or server-cache billing experiment was performed. This record does not prove zero defects, AGI, actual hit-rate improvements or a percentage cost reduction. Steps still require executor evidence; long tasks should declare top-level `goal_conditions`. Arbitrary natural-language success criteria cannot yet be converted automatically into complete observable acceptance checks.

The upgrade advances the memory envelope to format 13. Old steps without `prerequisites` retain their behavior. Old mechanism rules supported only by caller-named contexts require fresh physically independent support before automatic reuse. New data is not guaranteed readable by older engines.
