# v1.0.0 local validation record

Historical upstream validation, translated for the CraftDroid fork.

Date: 2026-11-03. This is the local development acceptance record for implementation commit `134bab4`; subsequent release-documentation merges do not change that implementation. GitHub release builds have separate source-commit and CI provenance in their releases.

## Environment and results

Windows / PowerShell, Temurin Java 21.0.13+9, Gradle 8.14.3, Fabric Loom 1.13.6, Minecraft 1.21.1, Fabric Loader 0.20.1, Fabric API 0.116.17+1.21.1; NeoForge 21.1.72 build.

| Validation layer | Passing result | Evidence relative to repository root |
| --- | --- | --- |
| Component regressions | **268 tests / 45 classes, zero failures or skips** | `fabric/build/test-results/test/TEST-*.xml` |
| Native baseline without external mods | Five scenarios | `fabric/build/gametest-results.xml` |
| storage | Six scenarios: five baselines plus one real drawer scenario | `fabric/build/modpack-storage-results.xml` |
| industry | Seven scenarios: five baselines plus two real industrial scenarios | `fabric/build/modpack-industry-results.xml` |
| combined | 12 scenarios: five baselines plus seven real-mod scenarios | `fabric/build/modpack-combined-results.xml` |
| Distribution acceptance | Both builds, version/classes/license checks, test/third-party exclusion and SHA-256 passed | `build/release-artifacts.sha256` |
| Automated summary | XML completeness, source freshness, no model transport attempts and artifact hashes passed | `build/validation-1.0.0.json` |

Native scenarios executed **30 times**, representing **12 unique scenarios**. Repeated baselines across environments are not 30 independent capabilities. Component coverage increased by 52 over v0.6.0's 216.

The source digest covers 381 implementation, test, resource and build files with LF-normalized text:

```text
source_sha256_normalized = a878bb76d6f2f8f3b66cad5896c4f1756bba3af89c66bbf458389ace19a31cc1
modpack_lock_sha256      = 24834b7660a8f7c7a3d7a068086a7a2e4caf7874a05b5b4b9226dfb7809187d7
```

This binds local evidence, not the complete repository/source archive. Documentation and Python helpers are outside the digest. After source changes, the summarizer rejects native reports older than sources and requires reruns.

## Actions actually exercised

- **Real drawers:** Production observations and budgeted `transfer_port` experiments on two Extended Drawers instances. Actual quantity changes support rules; exact reverse compensation restores items. After unrelated task B and JSON restoration into a new AgentLoop, transfer to a third held-out drawer A succeeds only after fresh target observations with matching face. Missing/wrong-face observations reject transfer; observing the top face permits verified execution.
- **Industrial furnace fallback:** An unconfigured Tech Reborn automation port rejects insertion without consuming input. Normal interaction opens the real menu; public acceptance/indices guide ore/fuel movement. Actual ticks produce a real smelting result, which is observed and removed into inventory; input is consumed.
- **Energy observation:** A real electric furnace exposes nonzero capacity, actual zero stored energy and `team_reborn_energy` units through Team Reborn Energy. Unknown capabilities/Fabric units are not forced into FE.
- **Cooking:** A real Farmer's Delight pot above a heat source processes actual rice/bowls through public menu indices and game ticks. Observe and collect cooked food. Public layout locates the anonymous bowl slot; acceptance alone is not recipe-role evidence.
- **Cross-mod movement:** Farmer's Delight rice enters Extended Drawers and is withdrawn in batches with total-count conservation.
- **Two-companion handoff:** Two real inventories and AgentLoops under one owner coordinate dependencies and leases. A successor cannot be claimed before its predecessor completes; another companion's port write is rejected during the lease. Real drawer counts verify supply, and receiving inventory verifies receipt.
- **Existing baselines:** Test-only unfamiliar-block learning/transfer/rule changes, actor UUIDs, tooltips, production observation chains and actual data reloads remain covered. Test-only blocks are not described as external real mods.

Actions run on a real headless Minecraft server with original author mod code. Scripts prescribe deterministic experiment/action sequences and pause model deliberation. Production modules perform observation, movement, learning, conditions, skills and teamwork. Scripted routes are not autonomous model discoveries.

## Component coverage and review

Added/expanded tests cover historical-rule pressure and rare-task retention, JSON recovery, legacy-condition downgrade, fingerprint/reload prevention of archived-rule revival, strict retrieval budgets, action success versus final acceptance, rejection of pre-task facts, zero-support teammate candidates and two independent local validations, protection of existing counterexamples, shared-state invalidation/cleanup, job dependencies/leases/takeover/exclusion, bounded retirement of completed jobs and relevant operators surviving unrelated-rule pressure.

Port regressions check action/effect coordinate identity, face/dimension binding, actual inputs, complete reverse compensation, noninteger parameters and invalid directions. Cache regressions check serialized Responses boundaries, Chat blocks, exclusion of live tails, unknown relay/local/old-model compatibility and no extra prewarming. Existing usage/missing-report checks pass.

Review also covers format-15 read/write/reset, lifecycle cleanup, default builds without external mods, both loader artifacts, workflow dependencies and source changes. New Fabric API resolves missing `c:eggs` / `c:mushrooms` tags from the old combination. Existing dependency/Gradle development and deprecation warnings remain; failures were not hidden to obtain passing results.

## Local artifacts

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| `mineagent-fabric-1.0.0.jar` | 1,605,962 | `9519192988351b401e812bc7ad2611e5186c7271cbdc34ff34feafe357a31e10` |
| `mineagent-neoforge-1.0.0.jar` | 1,612,159 | `90873a8b4a2c6b94b376655bc760cc32261d972bfa5b5705601d99985ae88ee0` |

Build time, toolchain and later CI rebuilds may change bytes. Verify downloads against the same batch's manifest. Real test mods and nested dependencies are excluded from both JARs.

## Reproduction

Initial runs need Internet access for public dependencies/toolchains; cached runs may add `--offline`. No keys or paid calls are involved.

```powershell
py -3 scripts/provision-modpack-tests.py --profile storage
py -3 scripts/provision-modpack-tests.py --profile industry
py -3 scripts/provision-modpack-tests.py --profile combined
.\gradlew.bat --no-daemon -Pmodpack_profile=storage :fabric:test :fabric:verifyModpackGameTests
.\gradlew.bat --no-daemon -Pmodpack_profile=industry :fabric:test :fabric:verifyModpackGameTests
.\gradlew.bat --no-daemon -Pmodpack_profile=combined :fabric:test :fabric:verifyModpackGameTests
.\gradlew.bat --no-daemon prepareReleaseNotes verifyReleaseArtifacts
py -3 scripts/summarize-validation.py
```

CI has a reusable workflow for all three pinned environments. Releases must pass that matrix before building source and loader attachments. This report records local results; cloud results and final hashes are in the corresponding release's Actions link/manifest.

## Limits

The user chose no live model endpoint or paid budget. **Autonomous model exploration/innovation/long-task success, server cache hit rates, response-latency improvements and actual billing savings were not measured.** Tests establish infrastructure behavior under the listed conditions.

Coverage is limited to these versions/scenarios, not arbitrary large modpacks, multi-day survival, complete unfamiliar-NPC gameplay or full technology-tree completion. NeoForge built successfully but lacks an equivalent real-mod runtime matrix and remains experimental. Passing regressions guarantee neither zero defects nor AGI. Future evaluations need fixed models/budgets and independent held-out tasks.
