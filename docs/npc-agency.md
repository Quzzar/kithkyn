# NPC agency

Approved direction, 2026-10-21. Track delivery on
[#185](https://github.com/Quzzar/kithkyn/issues/185). The goal is a resident whose
observations, choices, words, memories and physical actions agree. The Halo companion
demonstration is evidence that the interaction is possible, not a benchmark for a populated
Minecraft village.

## Responsibilities

Minecraft and Kithkyn's execution code own navigation, collision, timing, world changes and
item conservation. A bounded decision model may choose an offered goal or reaction; a
generative model handles language, summaries and longer deliberation. Neither can execute
an invented tool. Keep the decision backend independent of the dialogue provider.

One inference runtime can serve many residents, each with their own state. Dispatch decisions
on meaningful changes and addressed requests. A population increase must not produce one model
request per resident per game tick. Preserve immediate physical responses to danger.

## Implementation plan

### Validated actions and references: #186

First connect the existing player conversation to follow, wait, move to a pointed location and
resume. An offer captures reference positions before inference. The result selects an action
name; it cannot contain executable code, coordinates or an arbitrary entity identifier. Apply
it on the server thread after checking freshness, ownership, availability and ground. Track
attempts through completion or an explicit terminal result.

The initial implementation is experimental and disabled by default. In `kithkyn-common.toml`:

```toml
[llm]
"Experimental resident actions" = true
```

While looking at the top of nearby ground, press **G** (remappable as **Point out a location**)
before opening a resident's chat. The server captures the reference for sixty game seconds.
Ask naturally, for example "Go over there", "Come with me", "Wait here", or "You can get back
to work". The resident may agree or refuse in the existing generative conversation. A model
choice must pass the action validation even if its accompanying dialogue sounds confident.

Initial bounds:

- Pointing ray: 32 blocks, already-loaded terrain; first version uses top faces and solid
  standing support. The resident's collision box must fit at the destination.
- Conversation offer: 30 game seconds; a newer offer or ended action invalidates an older reply.
- Execution: at most five game minutes, within 48 blocks of the requester; follow and wait
  remain active until released, interrupted or expired. Arrival completes pointed movement.
- One requester owns an active intention. Another player cannot replace it through a late reply.
- Danger, essential recovery, sleep and village travel interrupt. Posted ranged guards, young
  children, merchants and raiders do not take these movement requests.
- Navigation refreshes at most twice per second. Repeated route failures or ten seconds without
  progress end the action visibly; this feature never teleports a resident to complete an order.
- An agreement is remembered as an agreement. Completed, failed, cancelled, interrupted and
  expired outcomes have distinct records and player feedback.

Active orders and pointing references are session-scoped in this first slice. Leaving the loaded
world interrupts an active order. Save/reload continuity and persistent unfinished goals are #189.
The availability flag is an experiment gate, not evidence that the current model has passed the
full companion scenario. Do not close #186 on unit tests alone.

Run `./gradlew check` for the lifecycle/parser tests and existing project checks. The physical
execution fixture is `./gradlew runResidentAgencyVerification`, in the isolated
`run/npc-agency-verification` directory. Before its first run, accept Minecraft's EULA there
and set `general."Generate villages" = false` and `llm."Enable LLM?" = false` in that directory's
`config/kithkyn-common.toml`. The fixture enables actions for its own run, uses a simulated
speaker and real registered resident goals, and does not perform inference. It checks the
pointing ray, arrival, follow/wait/release, approaching danger before injury, rejection during
existing avoidance, quarrel precedence, a sealed destination and removed ground. Read the
`[resident-agency-verify] RESULT PASS` marker; server shutdown alone is not a passing result.

### Grounded awareness: #187

The initial capture and conversation-grounding layer is implemented. Its exact provenance,
visibility/expiry limits, budgets and verification are documented in
[npc-awareness.md](npc-awareness.md). It makes no new model requests and does not yet choose
autonomous reactions. Populated-village and real-model acceptance remain open.

Reuse witness checks, personal logs and existing village context. Represent facts with source,
speaker/entity identity, time, expiry and whether they were seen, heard or reported. Begin with
addressed speech, visible threats, nearby injury and existing resident speech. Bound distance,
line of sight, buffer size and repeated-event coalescing. Evaluate only meaningful changes.

Prove that distant or occluded events are not remembered as witnessed, stale observations expire,
and repeated events do not flood inference. A blocked route must be distinguishable from a model
that selected the wrong goal.

### Personality in choices and tactics: #188

Add a separate typed decision capability for an explicitly configured local Kev or hosted Jev
endpoint. Build offers from the resident's real abilities, relationships, duties and observations.
Start with a small set of implemented reactions: continue, investigate, warn, seek help, defend
or retreat. A choice changes the goal, while existing execution mechanics carry it out.

Compare stable scenarios across personalities and providers. Calibrate confidence on those cases;
an invalid, uncertain, late or failed decision gets an explicit result and never silently changes
provider or invents an action. Call sites that require authored reasons need an explicit design
because Kev/Jev do not generate them. No automatic substitute policy is authorized by this plan.

### Memory and unfinished intentions: #189

Extend existing personal logs, relationship memory and undertaking semantics. Keep requested,
accepted, attempted and completed distinct. Persist intentions only when their lifecycle, expiry
and requester ownership are defined. Record a real failure before considering another approach.
Reconsider on relevant changes rather than repeatedly issuing the same impossible instruction.

Verify that later conversations recount actual outcomes, save/reload retains only the intended
commitments, and no resident gains another's private experiences as omniscient knowledge.

### Voice and physical presence: #190

Route explicit push-to-talk input into the same conversation and action path. Add stable resident
voice identity, subtitles, speaker position, distance, interruption and a bounded audio queue.
Player-facing dialogue takes precedence over ambient exchanges. Model/recording/playback failures
need visible status; do not silently replace the selected voice engine with a system narrator.

Choose the engine against distribution permission, actual supported platforms, memory and measured
speech-end-to-first-audio latency. Breeze TTS 2 is the reference demonstration. Its public code
license does not establish unrestricted distribution of its model, and its official CUDA runtime
does not provide a cross-platform zero-setup package. See the
[Halo stack research](research/halo-companion-stack-2026-10-21.md).

### Model choices: #191

Evaluate exact original and modified local models at the same quantization and runtime. Record
immutable revisions, checksums, size, permission and provenance. A reduced refusal count does not
establish better action selection, JSON correctness, memory or sustained conversation.

Verified candidates and known limitations are in the
[Heretic model research](research/heretic-local-models-2026-10-21.md). The existing recommended
defaults remain unchanged. The [initial paired replay](research/local-model-replay-2026-10-21.md)
records real-provider outputs and repeatable cases, including word/action mismatches and
invented context. It does not complete the in-game action or inventory acceptance work.
Test a smaller Kev decision model separately from generative models;
neither belongs in the other's provider contract merely because both run locally.

## Acceptance and delivery: #192

The end-to-end scenario is: call a resident over, point somewhere, ask them to wait, introduce
danger, observe an appropriate choice, and discuss what actually happened. Include refusal,
unreachable ground, changed references, interrupted work, a disconnected speaker and delayed
inference. Use the real configured model and actual game navigation.

Repeat with multiple residents and villages. Report action legality and completion, accurate
recall, request counts, queue growth, p50/p95 decision and speech latency, model memory, game tick
times and client frame times. Separate perception, model and execution failures. A compile pass,
a scripted response or a convincing single scene is not the full acceptance result.

Delivery order: action foundation, grounded awareness, decision capability and durable intentions,
voice/presence, then populated-village acceptance. Model evaluation can run alongside those stages.
The tracker holds progress; this document records the agreed direction and implementation contract.

The next implementation slice is the independent, default-off [resident reaction lane](npc-reactions.md).
It consumes witnessed evidence and offers only implemented effects, with game-owned routes,
freshness and confidence gates. Its model replay and game execution fixtures are separate evidence.
