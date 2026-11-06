# Live DeepSeek resident validation — 2026-11-06

The official endpoint authenticated successfully and returned `deepseek-flash` and `deepseek-v4-pro` in model discovery. The tests below used `deepseek-flash`, with thinking disabled, through the production OpenAI-compatible transport and the real NeoForge 1.21.1 agent loop. Credentials remained in runtime bindings; no key is included in test fixtures, repository files or evidence.

## Verified gameplay

A Survival resident received two oak logs and a request for a small marker. DeepSeek chose a local procedure, crafted one recipe batch into four oak planks, and placed three actual blocks at the requested positions. A log and the placed planks were consumed through normal crafting and placement. The test required the procedure to reach **SUCCEEDED**, rather than merely stop after placing blocks; a verified reusable skill was recorded.

The same resident used its memory tool to retain the orchard boundary, requested a perspective scene, received the actual server-rendered PNG through the API, and delivered the image description through the resident chat path. The software renderer used no Minecraft client, GPU renderer or human camera. Inspection of the saved image agreed with the description's close-up gray block surfaces and tan plank edge. This validates the simplified scene path, not client-quality scene understanding.

Two other residents discussed a possible town. They heard each other's ordinary resident chat and retained independent relationship notes about the other person's preferences and their own opinions. The notes distinguished personal judgments from mutual agreements. Both were paused, snapshotted, despawned and recreated through the normal snapshot loader; identities and both relationship notes survived. This exercised reload and body recreation within one server process, not a full JVM restart or long-term social evolution.

## Final measured run

| Scenario | API attempts | Completed responses with usage | Scenario time | API latency min / median / max |
|---|---:|---:|---:|---|
| Craft, build, memory and spoken vision | 5 | 4 | 18.2s | 1.87 / 2.18 / 2.70s |
| Two residents, relationship notes and reload | 7 | 5 | 9.0s | 1.66 / 2.17 / 3.96s |

The remaining attempts were cancelled when superseded or stopped. Their billing is not inferred from local completion status. Completed responses reported 103,853 input tokens, including 87,168 cached input tokens, and 1,816 output tokens across these two scenarios. These are measured API usage fields, not an invoice or a general price/latency guarantee.

Each live scenario admits at most twelve API attempts, including any failure or cancellation. It has a wall-clock deadline and uses an isolated disposable test world. The normal CI path is scripted and cannot spend API credits. The test provider preserves separate per-resident session monitors while sharing its attempt cap, matching the production concurrency design.

## Issues found and corrected

- The original local-work input left semantic conditions underspecified. DeepSeek first supplied prose, then string arrays, and later guessed unobserved fact keys. This cost retries and could delay verification after the physical work was already done. The tool now advertises typed `steps`, `tool`, `args` and condition objects, recommends omitting unnecessary semantic assertions, and retains legacy `steps_json` compatibility. The final live run's first procedure was accepted directly.
- Expected request interruption was initially counted as a provider failure by the test wrapper. Superseded requests are now recorded separately from genuine provider/HTTP errors.
- Loop removal previously waited for callbacks that needed the same server thread performing shutdown. Shutdown now invalidates the turn and interrupts HTTP and tool waits before bounded cleanup.
- The speech cooldown silently discarded later replies. A bounded queue now retains pending speech, clears it on pause/removal, and exposes the last delivered utterance for the probe to verify actual spoken image feedback.
- Both `DEEPSEEK_API_KEY` and `CRAFTDROID_DEEPSEEK_API_KEY` are accepted. The CraftDroid-specific binding takes precedence, then the standard binding, then configured credentials. Values are neither logged nor persisted by this resolver.

## Reproduction

```sh
# No API spending; runs the actual resident loop with a scripted provider.
./gradlew -President_probe=offline :neoforge:verifyResidentGameTests

# Explicit paid opt-in; requires an already configured runtime secret.
./gradlew -President_probe=live :neoforge:verifyResidentGameTests
```

The live run requires both named behavioral scenarios in its XML report, plus the core lifecycle and population scenarios. Missing scenarios, failures and skipped tests fail verification. JSON evidence is under `neoforge/build/gametest-probe-live/`; reports include tool results and scenario metadata, never authorization headers or PNG base64. Offline evidence is preserved by CI.

The latest regression checks also passed: 295 component tests; six Fabric native scenarios; two core NeoForge scenarios; both loader artifacts; and three NeoForge scenarios with checksum-pinned Mekanism, AE2 and GuideME.

## Limits

These are short, controlled capability tests. They do not establish self-directed town construction, long-running survival, weddings or evolving social histories, safe interaction with every mod, human-client skin appearance, or ten active agents on a 4GB host. Chunk simulation caps and broader unattended modpack soak testing remain separate work.
