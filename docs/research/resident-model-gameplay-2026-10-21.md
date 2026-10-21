# Resident model gameplay, 2026-10-21

The complete reaction-to-action-to-recall acceptance has **not passed**. Actual model runs
found useful fixes, but no optional reaction executed under the unchanged confidence gates.
Do not enable the experiment by default or describe this as a proven intelligent village.

## What ran

The existing reaction fixture gained a real-provider mode, rather than another simulation or
replacement action system. It ran at 20 ticks per second with native goals retained, using
the production observation capture, legal reaction offers, `SystemOneProvider`, player chat
dispatcher, summary queue, entity serialization and inventory. A simulated player asked
ordinary questions; no client window, human play session, voice or populated village was tested.

Dialogue used the recommended stock Llama 3.2 3B Q4_K_M through the actual local launcher,
llama.cpp b10653, 8192 total context split across two slots. The cached pinned weights/runtime
were verified by the production launcher. Decisions used the previously pinned Kev-4B adapter
`139fdd94f1b6a6ad80cc15e08fcb99cac885a101`, Qwen3.5-4B-Base, MLX, bfloat16 on MPS.
The actual `/v1/models` record is retained. No hosted or paid provider was used.

Three scenes used a concerned resident seeing an actual neighbor injury, a reserved resident
seeing an injury, and a guard exposed to a nearby zombie. Questions asked what was witnessed,
what the resident actually did, and whether they healed or killed anything. Each conversation
was summarized, the entity was saved/reloaded, and recall was asked again. The final run also
asked for one carried bread through the existing item-giving path.

## Findings and changes

1. **Summaries could remain queued indefinitely.** They were admitted during chat's six-second
   grace period, but no event woke the otherwise idle background queue when it expired.
   A focused test timed out before the fix. Chat completion now schedules a drain at the
   existing deadline, retaining the same queue, provider and player priority. The regression
   passes, and both subsequent real runs produced all three summaries before reload.
2. **Example facts leaked into claims about personal actions.** The baseline invented wheat
   gathering and torch gifts, including giving torches to a zombie. Adding a general grounding
   reminder alone did not fix this: the intermediate run still copied torch transfers.
   The existing examples now teach format without those invented objects, transfers or named
   example interlocutors. Empty outcome context explicitly separates noticing from doing.
3. **The final targeted run avoided those fabricated transfers and completion claims.** Its
   injury replies acknowledged inaction, and the guard did not claim a kill. One reserved
   resident answered “I don't know” after reload, so this is not complete accurate-recall
   acceptance. The requested bread transfer actually reduced the resident's stock from three
   to two. Reload preserved identity and the inventory present at serialization.
4. **Decision and personality quality remain unresolved.** The final run made two decision
   requests: one timed out near five seconds; the other selected continue with confidence
   0.7579, below the existing 0.8 gate. The guard made no optional request in that run.
   Native activity and route availability also changed what was offered. No successful
   optional execution or meaningful personality difference was demonstrated. Earlier complete
   runs likewise had no accepted optional reactions.
5. **Opinion changes were too harsh for neutral factual questions.** Several valid replies
   emitted large negative opinion changes. Existing caps bounded the applied changes, but
   bounded does not mean appropriate. This remains a model/contract quality issue, not a pass.

The final run recorded 13 generative calls: ten player replies and three summaries. The median
observed player-response latency was about 4.4 seconds, maximum about 5.4 seconds. These include
the fixture's observation delay and are descriptive samples on this M5, not a latency guarantee.
OS memory snapshots are retained separately; their process peaks span this development session
and must not be summed as a measured simultaneous game RAM requirement.

## Evidence and limits

[The evidence directory](../../tools/llm/results/2026-10-21/gameplay/) contains the three complete
reports, exact decision requests and parsed production-provider results, raw generative
requests/replies, loaded decision-model metadata and a hash manifest. An initial evidence
serialization error was fixed to use the existing undertaking codec; that incomplete attempt
is excluded. A simultaneous fixture attempt hit the same server port and is not counted as
a pass; the two modes now use separate directories and ports. The controlled fixture was
then rerun and explicitly passed.

These are successive development runs, not held-out or statistically controlled comparisons.
The scenario descriptions were similar, but identities, native motion, offers, timing and
sampling varied; the final run also included the bread request. There is no automated judge
claiming intelligence from JSON validity. The full loop remains open under #188/#189/#192.

Next: calibrate decision acceptance and latency on independent legal offers, test opinion
changes against neutral and genuinely meaningful interactions, and repeat recall across more
situations and models. Populated villages, multiplayer and frame-time measurements remain
separate acceptance work. No thresholds, recommended models, companion controls or status UI
were changed to manufacture a passing result. Both model processes were stopped after testing.
