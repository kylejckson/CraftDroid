# Continual-learning development branch validation: 2026-11-02

> This baseline record retained the 0.4.0 package name. Later development packages use 0.5.0-alpha.1; see the [corresponding validation record](validation-0.5.0-alpha.1.md). The hashes below identify that historical build, not the current development package.

This record describes the local `codex/adaptive-learning-runtime` branch, developed from `701250a950154667212d3ae1be3b3197e9ce9470`. No new release was published or pushed. See the [continual-learning design](continual-learning.md) for the design and sources.

## Environment and commands

- Windows; Temurin JDK / javac 21.0.13+9; Gradle Wrapper 8.10.2.
- User-level `JAVA_HOME`: `C:\Users\Administrator\.jdks\jdk-21.0.13+9`.
- Final validation: `gradlew.bat --no-daemon :fabric:build :neoforge:build`.
- Both loader builds succeeded. Shared-component regressions under Fabric: 30 suites, 136 tests, zero failures, errors or skips. NeoForge has no independent test sources; its production compilation and packaging were verified.
- `git diff --check` passed.

## Behavior covered

| Area | Regression evidence |
| --- | --- |
| Learning and exploration | Physical-context deduplication; isolation of parameters, before-values and environments; normalized predictions; 700 fixed-seed random interventions; history bounds; rule drift and checkpoint restoration |
| Compositional planning | Independently learned transitions form new routes; step limits; single observations cannot authorize reuse; counterexamples disable routes |
| Skill transfer | Actions and acceptance bind to a new target; success on an old target cannot complete a transferred skill; invalid, unknown and cyclic bindings are rejected; legacy per-step overrides remain usable |
| Evidence migration | Legacy caller-named contexts cannot restore confirmed adapters; internal audit facts cannot replace frozen probe observations; deeply nested or oversized arguments are rejected before lifecycle admission |
| Model protocols and cost | Built-in session types and isolation; concurrent local HTTP; synchronous compatibility for old extensions; stable Anthropic cache blocks; cache-write fields; normalization of abnormal usage |
| Action metrics | Tool queries are separate from physical actions; pending asynchronous work is not failure; terminal callbacks are idempotent; cancellation and insufficient evidence are separate outcomes |

Local HTTP tests do not connect to paid model services or measure real bills. Controlled semantic-state tests exercise production components, but are not complete Minecraft world tests.

## Build artifacts

Both JARs were checked for the new predictor, planning result types, binder, exploration tools, Provider interface, license and loader metadata. Version placeholders were expanded. Filenames retain the development baseline's `0.4.0` name and do not indicate a published update.

| Artifact | SHA-256 |
| --- | --- |
| `fabric/build/libs/mineagent-fabric-0.4.0.jar` | `D3DA4E01AF432404108F4246757352DDB5CEB559A2B4D3E23687671E74B0A0CE` |
| `neoforge/build/libs/mineagent-neoforge-0.4.0.jar` | `1AF656D7CC75F60A0D3905664EC525B17F1BE9C51366BDF5FBDB9DE35F933CF3` |

## Not yet verified

No long-duration Minecraft survival, genuinely unfamiliar mods, fair cross-project comparison, visual control or paid-endpoint cache experiments were run. This record does not establish AGI, absence of defects, improved learning success, actual cache hit improvements or cost reductions. The next live experiment should freeze the model and budget and separately measure initial learning, location transfer, recovery after rule changes and retention of old tasks; see the design protocol.

The initial NeoForge dependency download encountered an interrupted TLS handshake. Retrying and validating the cache against the official Maven source resolved it. Certificate verification and TLS security were not weakened.
