# Undertakings

The existing `kithkyn:undertakings` attachment is the single saved record for a resident's
matters and actual autonomous reaction outcomes. It feeds the existing personal log and
relationship reflection. There is no second memory database or model-written progress tool.

## Scope

Player companion commands were removed on 2026-10-21. There is no follow/wait/point/release
controller, saved executable player promise, route-reconsideration loop or player status
notification. The remaining lifecycle belongs to autonomous reactions.

`UndertakingService` is the transition and write boundary. Native execution records requested,
accepted, underway, completed, failed, interrupted, cancelled and expired as distinct phases.
A navigation goal starts UNDERWAY and only real arrival completes its walk. Warning completes
when speech is issued, without claiming a listener responded. Defense needs a visible end
before its deadline; expiry or target loss does not become victory. Continue goes directly
from requested to completed because no optional physical action starts.

Failed or interrupted records cannot restart themselves or become completed because of model
prose. They remain factual unfinished history until their original lifetime ends. Another
reaction requires fresh admissible evidence and a valid new decision.

## Saved history

Records retain stable ID, origin, valence, counterpart, milestones, notes and lifecycle.
Game-owned execution includes resident UUID, verb, dimension, captured destination/reference,
original age/assignment/post context and monotonic start/expiry times. Reactions have a
200-tick lifetime. Maintenance expires them conservatively on clock rewind as well.

Transient reaction perception and execution handles are not replayed after load. A saved
unfinished reaction becomes interrupted and requires fresh perception and a new decision.
No saved record provides a live entity pointer. Physical inventory uses its existing save path.

There are at most sixteen open and sixteen closed matters per resident. Admission fails
explicitly when open capacity is exhausted; open matters are never silently evicted. Closed
history trims oldest first. The codec validates executable verbs, ownership, coordinates,
lifetimes, phase/state consistency, duplicate IDs and bounds. Instant decisions cannot decode
as executing navigation.

Earlier narrative matters still decode and are labelled unverified; open ones are archived
without claiming completion. The retired PLAYER_REQUEST lane is read-only compatibility for
experimental history. No new player request is accepted, and an open retired request is
cancelled by maintenance, never resumed. Its old arguments are history only.

## Conversation and reflection

Typed undertaking entries share the existing personal-log memory cap, deduplication and
relationship reflection. Counterpart-scoped history remains private to that counterpart;
ordinary public witnessed events remain ordinary memories.

Chat receives a bounded authoritative view. Requested, accepted and underway are not completed;
failed or interrupted attempts remain failures or interruptions. `ChatHistoryData` records what
people said. Consolidation selects exact counterpart quotes into `ChatSummaryData`, preserving
questions and claims without generating new event prose. Earlier model-written summaries are
explicitly unverified and never become source quotes. Neither conversation store can overwrite
game-owned progress. This prevents summary rewriting, but does not prove that every generated
response interprets the records faithfully.

The old chat undertaking tool, word trigger, examples, open-to-advance coercion,
model-resolution standing bonus and audit/probe/selftest commands are deleted. The remaining
`/kkdev undertaking show <entity>` reads this store explicitly for developer inspection.
Technical reaction status is kept in logs/developer diagnostics, not player popups or chat lines.
Unusable dialogue output is logged; an empty acknowledgement releases the chat input without
fabricating NPC speech or an error/status transcript entry.

## Verification

`./gradlew check` covers factual phase transitions, owner/counterpart isolation, save round trips,
no false completion or automatic restart, bounded capacity, old narrative history, read-only
retired commands and malformed executable/chat contracts.

`./gradlew runResidentReactionVerification` uses its isolated `run/npc-reaction-verification`
directory and controlled decisions. Require `[resident-reaction-verify] RESULT PASS`, not
merely a successful Gradle exit. It checks real investigation and retreat movement, live
reference and duty changes, interruption and defense expiry without a false completion.
The removed player-command fixture is not retained. Real-model recall and populated
multiplayer acceptance remain open.
