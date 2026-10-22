# Experimental resident reactions

Part of [#188](https://github.com/Quzzar/kithkyn/issues/188). This is a bounded reaction
experiment, disabled by default. It does not replace native Minecraft goals or the
conversation LLM. Dialogue, personas, authored memories and existing village decisions
still use their existing text provider.

## Independent decision endpoint

The common config's `[reactions]` section enables the experiment and configures an existing
System One compatible endpoint. `Decision endpoint` is a base URL, initially
`http://127.0.0.1:8008`; the client appends `/v1/systemone`. `Decision model` defaults to
`kev-latest`. `Decision API key` is optional bearer authentication. Keep the config private
if it contains a key. Provider configuration changes require restarting the server.
There is no model downloader, process launcher, endpoint discovery or provider substitution.
A configured model name is an API identifier, not proof of the loaded weights.

The request contains one choice question, the exact legal keys and descriptions, one retained
observation, health, distance, occupation, duties, personality blurb, virtues and the current
opinion of the observed subject. Character data does not grant permission or create facts.
The response cannot supply coordinates, arguments, dialogue or generated reasons.

The client validates a single strict JSON object, duplicate keys, exact question/choice keys,
numeric finite probabilities, distribution sum, selected maximum and chance-corrected
confidence. Extra executable fields, unknown choices, truncated input, oversized bodies,
configuration errors, HTTP failures and five-second timeouts are explicit failures. There
are no retries. `Danger confidence` and `Injury confidence` both start at 0.8 for this
experiment. These are **uncalibrated** chance-corrected thresholds, not probabilities of a
safe action. A rejected decision starts no optional reaction; independent native goals
continue running throughout.

## What can happen

Game code captures references and reachable routes before requesting a choice, then checks
them again on the server thread. The following effects are implemented:

- `continue`: start no optional reaction.
- `warn`: speak an authored factual warning through the existing speech channel. No claim
  that anyone heard it or responded.
- `retreat`: attempt a captured native route away from a witnessed threat. Arrival means
  reaching that point, not being safe.
- `seek_help`: approach a visible guard from the same village and call out if the guard is
  still alive, nearby and visible. This does not order the guard to respond.
- `investigate`: approach a visibly injured fellow resident when no threat observation is
  retained. Arrival does not heal them or identify an unseen attacker.
- `defend`: set a currently visible hostile as the existing native combat target, only where
  the resident's existing proactive combat policy permits it. It never chooses a player,
  neutral creature, creeper or enderman as a new target. It does not assert a kill or victory.

Raiders, merchants, young children, sleeping/passenger/NoAI residents, village travelers,
and urgent native activities do not request reactions.
Guards assigned a fixed post (including ranged sentries, sword posts and jailers), and residents protected by the labor planner's essential staffing
rules do not receive optional movement routes. Custody remains in its existing player
policy; reaction defense never targets players. The native recovery, panic, combat and
travel goals have priority over reaction navigation. No inference request holds a goal flag.
A changed age, post, dimension, reference, destination or urgent need invalidates an offer
or interrupts a walk. Captured subjects and helpers are rechecked every ten movement ticks. Investigation also stops when a threat is noticed.

## Scheduling and visibility

Only SEEN threats and SEEN/EXPERIENCED injuries trigger this lane. HEARD/REPORTED speech
never does, preventing warnings from feeding another inference cycle. A continuously
refreshed sighting is one episode, with a bounded 32-episode deduplication set per resident.
An eligible resident checks on a staggered one-second schedule and has a ten-second
cooldown after admission. The server admits at most one request per second, has one request
in flight and no queued inference backlog. Busy residents defer without consuming an episode.
There is no population-wide reaction latency guarantee.

Offers expire after at most 100 game ticks and never outlive their captured observation.
Callbacks revalidate before applying anything. Movement expires after 200 game ticks and
uses bounded native path refresh/failure detection.
Perception visibility checks use the awareness system's shared budget.

`/kkdev reaction <entity>` reports the latest pending/deferred/uncertain/failed/stale/started/
completed/interrupted state when developer commands are enabled. The same bounded status
stays in explicit developer diagnostics. Conversation reads factual undertaking outcomes instead. Logs record outcomes without credentials or full
resident prompts. Reaction outcomes now use the existing [undertaking lifecycle](undertakings.md) and factual
personal log. Transient reaction execution is not resumed after reload; a fresh decision
is required.

## Verification and remaining acceptance

`./gradlew test` covers strict parsing, the HTTP request contract and bearer header, response
size limits, stalled-body cancellation, concurrent/closed providers, replayed/expired offers,
confidence gates and the live request builder. The synthetic replay uses that same builder:

```
KITHKYN_REACTION_REPLAY=/absolute/path/reaction-requests.jsonl ./gradlew test --tests '*ReactionReplayTest'
```

The optional `runResidentReactionVerification` profile uses `run/npc-reaction-verification`
as its disposable game directory and a controlled typed provider. Accept Minecraft's EULA
in that directory, and set `general."Generate villages" = false` and `llm."Enable LLM?" = false`
in its `config/kithkyn-common.toml` before running. Require the log's
`[resident-reaction-verify] RESULT PASS`; a successful Gradle exit alone does not prove the fixture passed. It exercises actual investigation and retreat navigation plus
invalid, uncertain, late, changed-child and disabled outcomes, a guard moving away mid-route,
last essential worker protection, eligible age/post changes and a warning without movement. The fixture also checks death at the exact defense deadline without claiming completion. This isolates game execution
from model quality. Player companion controls and their fixture have been removed.

Reaction requests now describe observed events, numeric traits with plain-language labels,
health and duties in readable text. Criteria describe the implemented action directly; game
validation, admission limits, freshness and confidence thresholds are unchanged. The replay
fixture retains the historical personality/order cases and adds eight separately authored
situations, explicitly labelled `independent`, rather than treating coordinate variants as
independent evidence. Capture with `KITHKYN_REACTION_REPLAY`, send sequentially using
`tools/llm/replay_reactions.py --source <capture> --output <raw>`, and score with
`tools/llm/analyze_reactions.py --source <capture> --raw <raw> --output <analysis>`.
Transport failures stop the replay without retry or provider substitution.

The [Kev research note](research/kev-reaction-evaluation-2026-10-21.md) records a separate pinned
local model replay: 38/48 authored labels matched, option order changed four of sixteen states,
and none met the fixed 0.8 confidence gate. The near-duplicate split is not independent validation.
No threshold or default was changed from these results. Model quality, per-purpose
calibration, larger populations in working villages, multiplayer, custody and posted guard
scenarios still need acceptance before enabling or recommending this experiment. #188 remains
open. The undertaking rework is tracked in #189; voice remains separate work in #190.

Run `./gradlew check` and the reaction fixture for current validation. The reaction fixture
uses controlled model results; this is not multiplayer acceptance. There are no added
action-status popups, HUD messages or technical status lines in player conversation.

For actual providers, run the same fixture with `-PreactionModels=true`. It uses
`run/npc-reaction-model-verification`, port 25685, normal 20-tick speed and native goals.
Enable the local stock Llama 3B dialogue provider in that isolated config, disable village
generation and autonomous villager conversations, and configure a real Kev endpoint. The
fixture sets final traits and occupation before rebuilding native goals through `reloadState()`,
asserts one reaction goal and the guard's native threat goal, and retains the 0.8 confidence gates and never injects a reaction or a chat answer.
It records three scenes, player-dispatcher conversation, summaries, reload and a requested
bread transfer. Its `RESULT RECORDED` marker means evidence exists, not that model quality
passed. See [the actual gameplay results](research/resident-model-gameplay-2026-10-21.md).
