# v0.5.0 release build validation

Validation date: 2026-11-02 (Beijing time). This record corresponds to `0.5.0` in `gradle.properties`, developed from `701250a950154667212d3ae1be3b3197e9ce9470`. See the [continual-learning design](continual-learning.md) for research, tool usage and limitations.

## Local build results

- Windows, Temurin JDK / javac 21.0.13+9, Gradle Wrapper 8.10.2.
- After cleaning output, Fabric and NeoForge compiled and packaged production sources successfully.
- Fabric shared-component regressions: 33 suites, 156 tests, zero failures, errors or skips.
- NeoForge has no independent test sources; validation covers compilation, packaging and artifact structure.
- `git diff --check` passed.

Command:

```powershell
.\gradlew.bat --no-daemon clean verifyReleaseTag prepareReleaseNotes verifyReleaseArtifacts '-Prelease_tag=v0.5.0'
```

`verifyReleaseArtifacts` depends on both loaders' `build` tasks, including Fabric tests. See the [development build record](validation-0.5.0-alpha.1.md) for coverage and the controlled 32-step scenario. The release reran the full suite against the same production components.

## Release consistency

- Version and tag arguments, generated notes, both exact JAR filenames and embedded loader versions all identify `0.5.0`.
- Passing `v0.5.0-alpha.1` makes `verifyReleaseTag` reject the release as expected.
- Each JAR is checked for the Provider interface, experience predictor, planning graph, skill binder, license and matching loader metadata.
- Release notes render `@VERSION@` from `.github/release-notes.md`; checksums are generated from actual artifacts.
- The release workflow repeats clean builds and these checks. Normal versions are full releases; versions containing `-` are prereleases.
- Remote execution status is recorded in [upstream GitHub Actions](https://github.com/bingdongni/CraftDroid/actions); historical attachments are in the [upstream v0.5.0 Release](https://github.com/bingdongni/CraftDroid/releases/tag/v0.5.0).

| Local Windows artifact | Bytes | SHA-256 |
| --- | ---: | --- |
| `mineagent-fabric-0.5.0.jar` | 1496662 | `c761d854cc1a77fdcecff10fbe3df7b270e8230b1940486f97b914866206e3ad` |
| `mineagent-neoforge-0.5.0.jar` | 1503241 | `fb0e59f5bbf41b11b9130439e6d5e3fa519b836a763a23fb15c4922da0b4c68a` |

This table identifies this local build only; identical bytes across platforms are not promised. GitHub Actions rebuilds from tagged sources. Verify downloaded attachments against `release-artifacts.sha256` from the same release. GitHub supplies source archives from the release tag.

## Validation limits

No long-duration Minecraft runtime, unfamiliar-mod blind test, fair cross-project comparison, visual control or paid-model cache billing experiment was performed. Passing component regressions does not prove zero defects, AGI, superior live learning or a percentage cost reduction. Long tasks must still declare top-level `goal_conditions`; one successful action cannot substitute for acceptance of the complete task.

The memory envelope is format 13. Old steps without `prerequisites` retain their behavior. Old mechanism rules supported only by caller-named contexts require new physically independent evidence for automatic reuse. Older engines are not guaranteed to read the new format.
