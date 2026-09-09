# Pet sitting diagnosis, 2026-09-06

Status: production fix active on the live development server after the user-authorized restart at
10:41 EDT. The model-disabled Minecraft regression and all 237 automated tests pass. The first
updated live pet decision recalled Peril; continued read-only monitoring is active.

## Confirmed movement bug

A villager-owned cat or wolf can sit without a sit order. Minecraft's `OwnableEntity.getOwner()`
looks up a player UUID, so it returns null for a Kithkyn villager. The mod separately resolves the
villager correctly in `CompanionPets.loadedOwner`, but previously left vanilla's `SitWhenOrderedToGoal`
installed. Its `canUse()` returns true for a tame, grounded, dry animal with a null vanilla owner,
before checking whether it was ordered to sit. Its priority is 2; the custom follow goal is 4.
The sitting goal stops navigation and sets the sitting pose. The follow choice and visible pose
can therefore disagree. The documentation's claim that vanilla owner-dependent goals quietly do
nothing does not hold for this sitting goal.

A copied Minecraft fixture on isolated port 25698 reproduced this with model use disabled:

```text
python3 run/pet-sitting-2026-09-06/run_probe.py baseline
cat: commandSit=false, vanillaOwnerNull=true, companionOwnerFound=true,
     vanillaSitCanUse=true, sittingPose=true, FAIL_UNREQUESTED_SITTING
wolf: same result
RESULT FAIL
```

The control changes only that sitting goal's eligibility inside the diagnostic: require an
explicit sit order before calling vanilla's checks. Both species then passed:

```text
python3 run/pet-sitting-2026-09-06/run_probe.py control
cat and wolf: vanillaSitCanUse=false, sittingPose=false
explicitSitWorks=true, recallClearsPose=true, followingGoalRunning=true
RESULT PASS
```

The real entity join handler and goal selector ran in both cases. This validates the causal
sitting conflict and preserves deliberate sit/recall in the probe. It is not a long-duration
navigation or live-world test. No live pet was moved and the active server was not restarted.
Probe labels retain their worlds/logs; use a new label for any repeat rather than overwriting.

## Separate model-decision problem

The live server uses the configured OpenAI provider, not the default local Llama used in the
redevelopment study. At the first captured sample, Magnus/Button had 39 pet decisions: 26 keep
sitting, one recall, then 12 keep following. Easton/Peril also had 39: 14 keep following, two sit
orders, 22 keep sitting and one recall. Joy/Moxie had one keep-following choice. These are model
choices from exact replies, not measurements of physical pose or time spent sitting.

Peril was told to sit at 03:12, recalled at 04:32, and seated again at 06:12. Later replies repeatedly
said resting was safe while the owner worked. Button was recalled at 08:32 and subsequently kept
receiving follow choices. Logs establish that sitting is not universally being ordered by owners.

`RealPerson.maybeOrderPet` asks at most once per game day. Before the fix, it constructed activity
from the occupation, describing employed owners as going about their work. It omitted current
movement, time spent sitting, separation from owner and observed nearby hazards. The prompt
suggested sitting could keep the pet safe, while following provided company. A repeated work/rest
situation therefore gave little new information that would motivate recall. The live replies
show this persistence. The new prompt has not yet been benchmarked against either provider.

Recall finds owned pets across the loaded dimension without a distance gate. It only selects the
nearest owned pet per daily opportunity, so an owner with both species may repeatedly consider
one. Missing or invalid answers preserve the current order. These are secondary limits, not
necessary explanations for the model-disabled sitting reproduction.

## Implemented correction and verification

`CoreEvents` now replaces the exact vanilla sitting goal with `CompanionSitGoal` for marked
companions, at the same priority. The extra eligibility check requires an explicit sit order.
It runs on fresh spawn and entity reload; ordinary player pets retain their vanilla goals.

`PetOrder` now receives observed movement/sleep, recent activity, pet distance, the current order,
rest duration, visible nearby hostiles and immediate fire/lava danger. Its guidance treats
following as normal and sitting as temporary, without inventing danger from a job title.
Rest starts are persisted on pets, retained across repeated sit orders and cleared on recall.
Old pets begin observation when first considered. Delayed replies are discarded when the order,
ownership, dimension or either participant's live state no longer matches the decision.
The model still chooses; this does not force existing deliberate sit orders to end on restart.
The daily cadence and nearest-owned-pet selection remain unchanged.

Validation:

- `./gradlew build --offline --console=plain`: PASS, 237 tests, zero failures/errors/skips.
- `python3 run/pet-sitting-2026-09-06/verify_production.py production-1`: PASS.
  This copies the current built production classes, uses a disposable world on port 25698,
  disables model requests, and enables `dev/CompanionPetVerification` without the diagnostic
  control override. The prior study runtime and baseline/control evidence remain frozen.
- Cat and wolf: uncommanded sitting prevented; explicit sit/recall preserved; repeated join
  does not duplicate goals; ordinary pets retain vanilla goals; NBT reload restores the fix.
- Both pets actually walked from eight blocks away to the stationary villager in 120 server
  ticks (cat ended 2.07 blocks away; wolf 2.24). Neither returned to uncommanded sitting.
- Rest timestamps survived NBT reload, repeated sit commands preserved elapsed rest, recall
  cleared it, and old saves without timestamps did not invent earlier rest history.

At verification, the live server was still PID 28933, started at 10:05 EDT before this fix was
compiled. It loads development classes directly from this project. A subsequent server restart
will load the fix for existing pets; no new world or client update is needed for these changes.
No live server restart or pet mutation was performed during this verification. Long-running model
choices and natural sit/recall frequency still need post-restart monitoring.

## Live activation, 10:41 EDT

The user authorized the restart. The old process (28933) received a graceful shutdown signal,
logged that all dimensions were saved, and exited before the replacement started. Gradle started
the server with the same verified pet class checksums; compilation was up to date. The new server
(PID 32267) reported ready on port 25565 at 10:41:04 and answered a Minecraft status ping.

At 10:41:14 the new live prompt described Peril as 57 blocks away with no visible hostile within
12 blocks, and included the updated following/rest guidance. Easton chose to recall Peril, and
the server logged the applied recall: “Peril is far away and no danger requires it to stay, so
call it back.” This was a natural production decision using the configured provider, with no
forced pet command or model test request. It confirms activation and one successful recall,
not a measured long-term sitting rate or a direct observation of live physical pose.

Restart metadata, exact pet class hashes, shutdown-save evidence and the post-restart snapshot
are retained under `run/pet-sitting-2026-09-06/restart-*`.

## Monitoring and evidence

The existing task heartbeat now monitors the live development server's pet choices and
redevelopment events. `run/pet-sitting-2026-09-06/monitor.py` reads logs and writes its own checkpoint;
it does not modify worlds, code, or server state. It records explicit sit/recall events separately
from retained posture choices. Physical pose is not present in those logs and must not be inferred
as measured. The paused, completed twelve-run redevelopment study remains intact.

The evidence directory contains exact pet requests/replies, baseline/control results, probe
sources, the initial live monitoring snapshot and checksums. Full isolated worlds and logs remain
under `run/pet-sitting-2026-09-06/`. The original diagnostic used the prior frozen runtime's unchanged pet
classes. Production verification instead used a copied snapshot of the newly built classes;
its class checksums and result are retained separately.
