# Resident awareness, reactions and memory

Track delivery on [#185](https://github.com/Quzzar/kithkyn/issues/185). The goal is villagers
whose observations, choices, words, memories and physical actions agree during ordinary
village life and the existing right-click conversation.

## Product boundary

The user removed player companion commands on 2026-10-21. Requiring a player to walk up and
right-click a villager does not support the proposed call-over/pointing interaction well.
Follow, wait, pointed movement and release commands are removed, including their keybinding,
network packet, config flag, chat action field, goals, status popups and dedicated fixtures.
Do not rebuild them as a remote-command layer or leave a disabled copy.

There is no added status HUD, action-bar notification, system transcript line or in-character
substitute for a failed model reply. An unusable reply is logged and the existing chat input
is released without adding a line. Real NPC speech continues through the existing conversation
and speech paths. Developer diagnostics remain available through explicit commands and logs.

## Responsibilities

Existing execution code owns navigation, collision, combat timing, world changes and item
conservation. An optional decision model chooses among implemented autonomous reactions;
the generative model handles conversation, summaries and existing village deliberation.
Neither can execute an invented tool. The decision endpoint stays independent of the dialogue
provider. Population growth must not become one inference request per resident per tick.

## Awareness: #187

The bounded observation log records nearby visible threats, injury, addressed speech and
existing resident speech. Facts retain source, identity, time and whether they were seen,
experienced, heard or reported. Reports are not verified world facts, and an unseen attacker
is not identified merely because a victim was visible. Witness checks, buffers, fan-out and
shared budgets bound the work. Awareness makes no model calls.

See [npc-awareness.md](npc-awareness.md) for exact limits and the actual-world fixture.
Populated gameplay and real-model use of this evidence still need acceptance.

## Autonomous choices: #188

The independent default-off [reaction experiment](npc-reactions.md) offers continue, warn,
retreat, seek help, investigate and legal native defense. Game code owns routes, references,
duties and permitted targets. Native panic, recovery, combat and travel take precedence.
Confidence, freshness and live-reference checks can reject a choice without executing it.
A warning does not order a guard to help; approaching an injured resident does not heal them.

The shared request budget, episode deduplication and resident cooldown bound inference.
No uncertainty or provider failure silently switches endpoints or invents another action.
Model quality and confidence calibration remain acceptance work.

## Factual memory: #189

The existing [undertaking store](undertakings.md) records game-owned reaction lifecycle and
actual outcomes, feeding the existing personal log and relationship reflection. Dialogue and
summaries receive these authoritative facts; a spoken claim cannot complete an action.
Saved history remains history. An unfinished reaction is interrupted after reload and needs
fresh perception and a fresh decision. No player-follow promise or route-reconsideration
notification remains. The old model-written undertaking tool and its completion reward stay removed.

## Voice inside the existing interaction: #190

Any later push-to-talk input should use the existing right-click conversation, with stable
resident voice identity, subtitles, speaker position, interruption and a bounded audio queue.
It must not restore the removed companion controls or add action-status UI. Coordinate
player conversation and ambient speech so nearby villagers do not all speak at once.

Choose an engine using measured platform support, distribution permission, memory and
speech-end-to-first-audio latency. The [Halo stack research](research/halo-companion-stack-2026-10-21.md)
is a reference, not a promise to ship that stack. No voice engine has been selected or bundled.

## Evaluation and acceptance: #191 / #192

Evaluate stock and modified local models at matched quantization and runtime, preserving exact
prompts, revisions and raw responses. Neither fewer refusals nor a valid JSON object proves
better behavior. Previous [paired replay](research/local-model-replay-2026-10-21.md) and
[Kev replay](research/kev-reaction-evaluation-2026-10-21.md) results are historical; changed
prompts and removed companion tasks are not newly measured by those results.

The acceptance scenario uses ordinary village play: witness danger or injury, observe a valid
personality-appropriate reaction, then use the existing right-click conversation to discuss
what actually happened. Include blocked ground, interrupted work, occluded evidence, refusal,
delayed inference and provider failure. Repeat with multiple residents, villages and players.
Measure request counts, decision and dialogue latency, memory, queue growth and tick/frame times.
Separate model mistakes, missing observations and navigation failures. Controlled fixtures,
compilation and an isolated model benchmark do not close that acceptance work.
