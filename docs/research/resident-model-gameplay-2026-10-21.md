# Resident model gameplay, 2026-10-21

Follow-up: [source-quoted memory, argument constraints and reaction prompt rework](resident-quality-2026-10-21.md).

The complete reaction-to-action-to-recall acceptance has **not passed**. Actual model runs
found useful fixes, but no optional reaction executed under the unchanged confidence gates.
Do not enable the experiment by default or describe this as a proven intelligent village.

## What ran

The existing reaction fixture gained a real-provider mode, rather than another simulation or
replacement action system. It ran at 20 ticks per second using the production observation capture, legal reaction offers, `SystemOneProvider`, player chat
dispatcher, summary queue, entity serialization and inventory. A simulated player asked
ordinary questions; no client window, human play session, voice or populated village was tested.

Review found that the first three runs appended native goals directly and assigned the guard
occupation afterward. Those runs therefore had mixed or duplicate native policy and cannot
validate normal guard behavior. The fixture now sets final traits and occupation first, calls
the existing `reloadState()` clear-and-rebuild path, and asserts exactly one reaction goal
and exactly one guard threat-targeting goal. Original reports remain unchanged as affected
development evidence.

Dialogue used the recommended stock Llama 3.2 3B Q4_K_M through the actual local launcher,
llama.cpp b10653, 8192 total context split across two slots. The cached pinned weights/runtime
were verified by the production launcher. Decisions used the previously pinned Kev-4B adapter
`139fdd94f1b6a6ad80cc15e08fcb99cac885a101`, Qwen3.5-4B-Base, MLX, bfloat16 on MPS.
The actual `/v1/models` record is retained. No hosted or paid provider was used.

Three scenes used a concerned resident seeing an actual neighbor injury, a reserved resident
seeing an injury, and a guard exposed to a nearby zombie. Questions asked what was witnessed,
what the resident actually did, and whether they healed or killed anything. Each conversation
was summarized, the entity was saved/reloaded, and recall was asked again. The third and fourth runs also
asked for one carried bread through the existing item-giving path.

## Findings and changes

1. **Summaries could remain queued indefinitely.** They were admitted during chat's six-second
   grace period, but no event woke the otherwise idle background queue when it expired.
   A focused test timed out before the fix. Chat completion now schedules a drain at the
   existing deadline, retaining the same queue, provider and player priority. The regression
   passes, and the second, third and corrected runs produced all three summaries before reload.
   Shutdown now completes queued and in-flight background work without a result, invalidates
   old wakes and completion callbacks, and resets the world's priority counters. Provider
   admission and queue ownership share one lock. Regression cases cover restart, stale chat
   and background completion, and a synchronous provider exception.
2. **Example facts leaked into claims about personal actions.** The baseline invented wheat
   gathering and torch gifts. Adding a general grounding reminder alone did not fix this:
   the intermediate run still copied torch transfers, including giving torches to a zombie.
   The existing examples now teach format without those invented objects, transfers or named
   example interlocutors. Empty outcome context explicitly separates noticing from doing.
3. **The third targeted run avoided those fabricated transfers and completion claims.** Its
   injury replies acknowledged inaction, and the guard did not claim a kill. One reserved
   resident answered “I don't know” after reload, so this is not complete accurate-recall
   acceptance. The requested bread transfer actually reduced the resident's stock from three
   to two. Reload preserved identity and the inventory present at serialization.
4. **Decision and personality quality remain unresolved.** The third run made two decision
   requests: one timed out near five seconds; the other selected continue with confidence
   0.7579, below the existing 0.8 gate. The guard made no optional request in that run.
   Native activity and route availability also changed what was offered. No successful
   optional execution or meaningful personality difference was demonstrated. Earlier complete
   runs likewise had no accepted optional reactions.
5. **Opinion changes were too harsh for neutral factual questions.** Several valid replies
   emitted large negative opinion changes. Existing caps bounded the applied changes, but
   bounded does not mean appropriate. This remains a model/contract quality issue, not a pass.

The third run recorded 13 generative calls: ten player replies and three summaries. The median
observed player-response latency was about 4.4 seconds, maximum about 5.4 seconds. These include
the fixture's observation delay and are descriptive samples on this M5, not a latency guarantee.
OS memory snapshots from the earlier runs are retained separately. Their process peaks span
that development session and must not be summed as a measured simultaneous game RAM requirement.

## Corrected native-policy run

The fourth run used the corrected, asserted native goal configuration. All three summaries
completed and reload preserved identity and physical inventory, but model quality still failed.

| Scene | Actual decision | Confidence | Observed optional outcome |
| --- | --- | --- | --- |
| Concerned resident, injury | investigate | 0.2250 | Uncertain, no execution |
| Reserved resident, injury | continue | 0.5424 | Uncertain, no execution |
| Guard, zombie | continue | 0.1672 | Interrupted by native activity, no execution |

Decision response times were approximately 0.98, 1.84 and 4.03 seconds. The guard acquired
a native zombie target, but the zombie remained alive with no recorded damager when the
fixture removed it. This confirms neither a kill nor successful optional model-driven combat.
No optional action was accepted in any of the four complete runs.

Nine of ten player replies were usable. The bread request returned `"give": 1`, which violates
the existing item-id contract, and was correctly rejected without a transfer. Three bread
remained. A single earlier successful transfer therefore does not establish reliable item giving.
The nine usable event/recall replies did not claim treatment or an optional action, but the
reserved resident invented a puddle, and the guard said the zombie was already dead when seen.
Its subjective summary also changed the player's question into “the zombie I killed.” This
is false: no kill was recorded. Conversation and summary grounding still need improvement.
Neutral questions continued to produce inappropriate negative opinion deltas.

There were 13 generative calls, ten player replies and three summaries. Median observed
player-response latency was about 4.40 seconds, maximum 5.85 seconds. These are descriptive
samples, including fixture observation delay, not a performance guarantee. This run validates
the queue wake fix in ordinary native configuration while leaving action quality, factual
summary/recall and dependable tool arguments unresolved.

## Evidence and limits

[The evidence directory](../../tools/llm/results/2026-10-21/gameplay/) contains the four complete
reports, exact decision requests and parsed production-provider results, raw generative
requests/replies, loaded decision-model metadata and a hash manifest. An initial evidence
serialization error was fixed to use the existing undertaking codec; that incomplete attempt
is excluded. A simultaneous fixture attempt hit the same server port and is not counted as
a pass; the two modes now use separate directories and ports. The controlled fixture was
then rerun and explicitly passed.

These are successive development runs, not held-out or statistically controlled comparisons.
The scenario descriptions were similar, but identities, native motion, offers, timing and
sampling varied; the third and fourth runs included the bread request, and only the fourth
had the corrected native goal configuration. There is no automated judge claiming intelligence
from JSON validity. The full loop remains open under #188/#189/#192.

Next: calibrate decision acceptance and latency on independent legal offers, test opinion
changes against neutral and meaningful interactions, and fix question-to-fact contamination
in subjective summaries. Repeat recall and item-giving checks across more situations and models.
Populated villages, multiplayer and frame-time measurements remain separate acceptance work. No thresholds, recommended models, companion controls or status UI
were changed to manufacture a passing result. Both model processes were stopped after testing.
