# Undertakings

Undertakings are the one saved store for a resident's commitments and their actual outcomes.
This reworks the existing `kithkyn:undertakings` attachment for [#189](https://github.com/Quzzar/kithkyn/issues/189).
It replaces both the temporary movement intention store and the earlier model-authored
open/advance/resolve tool. Dialogue does not edit physical progress or declare completion.

## One write path

`UndertakingService` validates transitions, then commits them to the existing attachment
and `PersonalLogData`. Those factual memories already feed relationship reflection; there
is no new memory database or separate standing reward. `ChatHistoryData` remains the record
of what people said, and `ChatSummaryData` remains subjective conversation memory. Neither
is evidence that a physical action happened.

Each undertaking retains its stable ID, origin, valence, counterpart, progress markers,
notes and terminal history. Its optional game-owned execution records the resident UUID,
player-request or reaction lane, implemented verb, requester, dimension, captured destination
and reference, original age/assignment/post constraints, monotonic start/expiry times and
whether an availability change has already been reconsidered.

The detailed phases are:

```text
REQUESTED -> ACCEPTED -> UNDERWAY -> COMPLETED
                |           |
                +-----------+-> FAILED / INTERRUPTED
                                    |
                           fresh relevant availability fact
                                    |
                         one recorded reconsideration
                         (no automatic execution)

unfinished -> CANCELLED or EXPIRED
```

A rejected request has never been accepted. An accepted promise has not yet started. A
movement goal records UNDERWAY when native navigation starts, and only actual arrival can
complete its walk. An authored warning completes when it is spoken; it does not claim that
anyone heard or helped. Defense records target engagement, then requires a witnessed end
before claiming completion; losing the target or timing out records interruption instead.

Failed and interrupted accepted attempts remain unfinished matters until their original
lifetime ends or the requester supplies a fresh instruction. A new instruction explicitly
cancels earlier unfinished promises to that requester rather than merging them. Releasing
a request completes only the release; it cancels the previous instruction.

## Persistence and ownership

`ResidentActionState` now validates transient offers and derives its current order and
latest outcome from saved undertakings. It has no separate current-order or outcome store.
A normal unload preserves the promise. On reload, native execution requires the original
resident, nearby available requester, same dimension, unchanged captured duties, usable
standing ground and an unexpired lifetime. Permanent removal cancels an active request.
Unavailable requesters or changed duties interrupt it without claiming completion.

Reactions use the same saved lifecycle. Their transient perception and execution handles
are not replayed after load: an unfinished reaction becomes interrupted and needs fresh
perception plus a new valid decision. No saved record supplies a live entity pointer.

Player promises retain their original 6,000-game-tick lifetime; reactions retain their
200-tick execution bound. Expiry also handles a monotonic clock rewind conservatively.
There are at most sixteen open and sixteen closed matters per resident. Admission fails
explicitly when open capacity is exhausted. Active promises are never silently evicted;
closed history trims oldest first. The codec rejects unknown executable verbs, invalid
coordinates/lifetimes, missing player ownership, duplicate IDs and oversized lists.

## Reconsideration without an implicit retry

The existing movement controller examines one paused promise at a time on a staggered,
five-second schedule, only when optional player actions are enabled and urgent needs permit.
The server admits at most one such path probe per tick. Other residents' world state is
not consulted. A requester must be visibly present; a fixed destination must still be
loaded, usable and have a path to the exact captured point.

When those relevant facts become available again, the same undertaking records one
reconsideration and notifies its requester. The prior FAILED or INTERRUPTED phase remains.
The marker persists through reload, so the same availability does not create a loop.
No movement, substitute destination, inventory transfer or inference request follows from
that notification. A new valid decision is required to start another attempt.

## Conversation and earlier saves

Player and resident dialogue receive a bounded authoritative view of these records; player
context includes only that counterpart's matters and the resident's own event reactions.
Reaction requests receive the latest three records. Session summarization also receives
the recorded lifecycle, with instructions that a promise or quoted completion claim cannot
be upgraded to a verified outcome. These are prompt-grounding guarantees, not proof that
every generative response will faithfully recount the facts.

The obsolete chat `undertaking` tool, word trigger, few-shot variants, open-to-advance
coercion, model-resolution standing bonus and old audit/probe/selftest commands are removed.
Malformed or unsupported dialogue output now reports a chat error and records no fabricated
substitute exchange. The remaining `/kkdev undertaking show <entity>` reads this same store.

Earlier saved narrative matters still decode. They are explicitly labelled unverified;
open ones are archived without declaring completion. This preserves history while preventing
an old model claim from authorizing work or rewarding a physical accomplishment.

## Verification

`./gradlew check` includes codec round trips, owner/counterpart isolation, exact lifecycle,
no false completion, one reconsideration across reload, explicit cancellation, old-save
history and malformed executable/chat contracts. The existing movement fixture is extended
rather than replaced by another parallel harness:

```
./gradlew runResidentAgencyVerification
```

Use its isolated `run/npc-agency-verification` directory, accept Minecraft's EULA there and
set `general."Generate villages" = false` and `llm."Enable LLM?" = false` in its common config.
Require `[resident-agency-verify] RESULT PASS`, not merely a successful Gradle exit. It
serializes and reloads actual entities/attachments, verifies resumed native arrival and
unchanged physical inventory, retains a failed route through reload, rejects stale summary
claims, observes one reconsideration after the route changes and requires a fresh approved
attempt before movement resumes. Decisions are controlled; this is not a real-model or
multiplayer acceptance claim. The reaction fixture independently checks the shared lifecycle
and movement safety.
