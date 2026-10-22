# Resident quality rework, 2026-10-21

This follows the [initial actual-model findings](resident-model-gameplay-2026-10-21.md).
Conversation memory now preserves selected source quotes, and local reply generation constrains
item arguments. Reaction choices improved in a labelled replay without lowering thresholds.
One final actual-model scene completed observation, choice, physical approach and accurate
post-reload description. This remains experimental work, not acceptance for populated villages
or multiplayer.

## Changes

- The existing conversation attachment now saves up to four exact counterpart quotes. The model
  chooses source indices instead of writing a first-person event story. Invalid references,
  duplicate references and added prose are rejected. Questions and claims stay attributed words.
  Actual event/action history remains in the existing personal log and undertakings.
- Earlier summary strings decode as explicitly unverified history, never source quotes. The next
  successful consolidation replaces them. Decode bounds cover eight counterparts, four quotes
  of at most 2048 characters, and old prose of at most 8192 characters per counterpart.
- The bundled llama.cpp request carries a local JSON schema. Gift IDs are strings from carried
  inventory; a quantity cannot appear without an item. Complete requests retain this constraint
  through chat, background queueing and repeat resampling. Shared parsing and physical transfer
  remain authoritative. Cloud protocols are unchanged and were not exercised against paid APIs.
- Opinion changes require a typed social reason, the current speaker's entire line quoted
  exactly, and a delta with consistent polarity. The local grammar generates that judgement
  before the NPC reply. Source validation and ordering do not prove semantic classification.
- Reaction requests use readable event/trait context and direct action descriptions, retaining
  numeric trait values. Confidence gates, native safety, legal offers and freshness are unchanged.

The local format follows llama.cpp's [schema response handler](https://github.com/ggml-org/llama.cpp/blob/master/tools/server/server-schema.cpp)
and [structured-output tests](https://github.com/ggml-org/llama.cpp/blob/master/scripts/server-test-structured.py).
Actual bundled-runtime runs below verify the path used here; prompt instructions alone do not
provide these argument constraints.

## Legal reaction replay

The corpus contains the original sixteen authored states in three option orders, plus eight
separately authored situations in three orders. The old `held_out` label is retained for
historical comparison, but those coordinate variants are not independent statistical validation.
The new situations were frozen before querying the model. Labels express expected reasonable
choices for these explicit scenarios, not an external expert judgement of village intelligence.

Review caught three new cases outside live health or visibility admission. Their health/distance
was corrected, and the entire corpus was regenerated and replayed. Only that corrected capture
is retained here. The fixture asserts the shared minimum health and visible-distance limits.

The pinned Kev-4B adapter/runtime and MLX BF16 backend are the same as in the earlier report.
All 72 requests used exact production request builders, sequential HTTP, three option orders,
no retries or substitutions, and the unchanged 0.8 confidence gate.

| Set | Requests matching labels | Choices above the confidence gate | Accepted label mismatches |
| --- | --- | --- | --- |
| Original sixteen states | 42/48 | 5/48 | 0 |
| Eight separately authored states | 24/24 | 9/24 | 0 |
| Total | 66/72 | 14/72 | 0 |

The earlier prompt matched 38/48 original labels and accepted none. Here all 24 states chose
the same action across their three option orders; one state crossed the acceptance gate only
in some orders. Fourteen accepted samples are far too few to call confidence calibrated or
justify enabling reactions by default. Six low-confidence choices still missed their labels.

Measured replay HTTP median was about 0.52 seconds, nearest-rank p95 1.94 seconds, maximum
4.34 seconds. These are local development samples, including cache effects, not game latency
guarantees or an isolated hardware benchmark.

## Intermediate game run

At source `1789e6cf`, three actual-game scenes used the stock local Llama 3.2 3B Q4_K_M and
actual Kev endpoint, with ordinary native goals and simulated right-click conversation.
All three consolidations selected source quotes and survived entity reload. A one-bread request
removed one of three bread from the resident and added one to the simulated player's inventory.

Twelve of thirteen player replies were usable. The invalid reply had an orphan `give_count`
without an item. The final schema now uses distinct gift/no-gift branches to forbid that shape.
Nine of ten usable neutral factual questions had zero opinion change; the remaining guard
recall wrongly returned -3. A genuine insult returned -5, and an apology returned zero.
Neither factual quote storage nor a valid JSON response guarantees appropriate social judgement.

The reserved resident confidently selected continue, recorded as completed without optional
movement. The concerned resident's investigation remained below threshold, and the guard request
timed out. This is not successful physical-action acceptance.

## Opinion attribution and runtime field order

The first typed-opinion contract required a kindness/apology/insult/threat kind, an exact quote
of the current speaker's line and a signed delta. It rejected invented evidence and inconsistent
polarity, but the actual model still misclassified neutral questions and praised a real insult.
Those results are preserved as `sourced-opinion-*`, not counted as semantic success.

Moving optional opinion before required speech in JSON property order alone did not enforce
that generation order. The [bundled converter](https://github.com/ggml-org/llama.cpp/blob/b10653/common/json-schema-to-grammar.cpp#L711-L761)
emits required fields before optional fields. The corrected schema uses separate branches where
opinion is absent or required ahead of speech. Gift/no-gift branches remain distinct so counts
cannot appear without an item. Runtime outputs must confirm the order; a Java map-order test
alone does not prove it.

## Final actual-model run

At source `d319abf3`, the native runtime actually emitted the sourced opinion before speech.
The captured insult response begins with `"opinion"`, attributes the exact insult to the player,
and applies -4. Fourteen neutral factual questions produced zero opinion changes. An apology
produced zero. All sixteen player replies were usable, and all four quote consolidations were
available before real entity serialization/reload.

| Scene | Actual model result | Game outcome |
| --- | --- | --- |
| Concerned resident, injury eight blocks away | investigate, confidence 0.7559 | Uncertain, no optional action |
| Reserved resident, same distance | continue, confidence 0.8493 | Continue recorded, no optional movement |
| Guard, nearby zombie | Timeout near five seconds | Native activity interrupted the optional episode |
| Inquisitive resident, injury three blocks away | investigate, confidence 0.9141 | Physically approached and completed |

The inquisitive resident moved from `(100.5, 161, 100.5)` to approximately
`(104.70, 161, 100.50)`. The authoritative execution says arrival, with no treatment performed.
Before reload she described approaching to observe; she denied healing. After reload she again
said she approached without treating the injury. This is one successful bounded full-loop case.
The closer distance and more explicit persona both changed; it is not a personality-only comparison.

The final bread reply omitted `give`, so it caused no transfer and all three bread remained.
Its wording, “I can give you 1” followed by having none extra, was inconsistent. Earlier complete
runs physically delivered one bread and preserved stock/inventory conservation, but this final
run does not establish dependable transfer selection. Generation constraints ensure argument
shape and ownership candidates, not willingness, good wording or correct intent.

Median observed player-response latency was about 5.23 seconds, maximum 7.60 seconds, including
fixture observation delay and an occasional resample. There were 21 generative calls: sixteen
player replies, four source selections and one resample. These are local development timings,
not an isolated performance benchmark or a village-scale latency guarantee.

Remaining problems include occasional perception denials despite recorded observations,
inconsistent gift wording/selection, model-sensitive social judgement and decision timeouts.
Positive social changes were not demonstrated by the final run. Confidence remains uncalibrated
outside this small authored corpus. Reactions remain off by default.

## Evidence and limits

[The evidence directory](../../tools/llm/results/2026-10-21/quality/) retains the corrected corpus,
exact decision request/response bodies, analysis, endpoint model metadata and complete game
reports with logical prompts/schemas and raw generative replies. The manifest records byte sizes, hashes and source commits. Identities are synthetic. The
existing local delegate expands the configured chat token budget to a minimum 1024-token wire
cap; logical call-log budgets must not be read as that HTTP limit.

An earlier overlapping Gradle invocation removed class files while a test and game were using
them. Those failed/incomplete attempts are excluded. Builds and game runs were then serialized.
Review also led to counterpart/legacy decode limits and removal of an unused schema-dropping
constructor. No companion controls or player status messages were added.

The game scenes use a disposable flat platform, a stationary injured entity and a stationary
zombie. Observer native goals remain registered; village economy updates are suppressed. The
player is simulated and no client UI is rendered. Raw conversation history remains after
reload, so this does not isolate quote memory from history or test a new Minecraft day. A
successful single resident interaction does not complete #188/#189/#192 acceptance.

Verification: 729 Java tests pass, six Python replay-scoring tests pass, and the controlled
actual-game reaction fixture reports PASS. Standards and spec reviews found and resolved decode
bounds, impossible replay admission cases and the native required/optional field-order issue.
The final model run reports RECORDED rather than an automatic quality verdict. Both model
processes were stopped after capture.
