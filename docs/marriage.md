# Marriage

**Implemented.** Two villagers who have grown close ask the village brain
to wed them, and the brain decides, the same way a villager petitions it to build
something ([villager-requests.md](villager-requests.md)). A married pair is given a
home of its own to raise. The whole feature rides rails that already existed:
pairwise bonds ([relationships.md](relationships.md)), the brain's `decide` call
([llm-brain.md](llm-brain.md)), and the saved-for goal
([buildings.md](buildings.md)). It invents no parallel state.

## The loop

```
a strong, mutual bond
   → each villager files a proposal naming the other        (MarriageProposals)
   → the brain is asked to bless the pair that named each other   (LlmService.decide)
   → the blessed couple settle their married name themselves      (MarriageNaming, on Dialogue)
   → both become MARRIED and take the name they chose (or a hyphenation), the pair edge is flagged married
   → spouses claim a free couple room in an existing home        (Village.houseCouple)
   → if none is free, the village saves for a home with a pair     (VillageGoal)
   → their shared chest follows for free                          (PersonalChest)
```

Each arrow is an existing mechanism. Nothing here is a new subsystem.

## Proposing is emergent, not scripted

A proposal is not raw feeling, and it is not the brain's decision: it is one
villager's expressed wish, the marriage twin of a build request. A single villager
whose bond to another single villager has grown **strong and mutual** files a
proposal naming them (`MarriageService.fileEmergentProposals`). Because a bond is
one shared object ([relationships.md](relationships.md)), a strong one makes *both*
sides file, and two proposals pointing at each other are the mutual ask the brain
is handed.

- **Strong** is an effective opinion of at least `STRONG_BOND` (50) on **both**
  sides. Generation caps a first-day opinion at 60 and drift is bounded, so a pair
  this warm both ways is among the strongest bonds a village holds: the feelings
  the model authored with a reason, not the slow pull of familiarity. The number is
  tunable; the live web is what settles it.
- Each villager proposes only to the **one** villager they are most fond of, so no
  one asks two people at once.
- Any two eligible, unrelated **Adults** may marry. There is no gender rule; Toddler, Kid,
  and Teenager stages never propose or appear in a marriage decision. A parent and child,
  full siblings, and half-siblings are rejected at proposal selection, mutual-proposal
  selection, naming, and the final wedding guard.

Proposals live in the brain's `strategy` tag beside the build requests and the
goal (`MarriageProposals`), age out after 1800 village-seconds, and are cleared
the moment the pair is wed.

## The brain decides

When two villagers have each proposed the other, the brain is asked whether to wed
them through the same numeric-choice `decide` the build planner uses: the facts of
the pair go in (who they are, their bond value and its flavour line, the village's
size), the model answers *yes, they should marry* or *not yet*. One decision is in
flight at a time (`Village.marriageDecisionPending`), the same discipline the build
and job decisions keep, so a slow model cannot stack requests. A failed, slow, or
absent model just leaves the pair betrothed for next pass; nothing is forced.

A wedding (`MarriageService.wed`):

- sets both villagers to `MARRIED` and gives them **one household name**, the one
  the couple chose for themselves (see "The couple names itself" below), or a
  hyphenation when their talk settled nothing.
- flags the `RelationshipPair` between them **married**, which is the single source
  of truth for who is wed to whom, and stops the pair from ever being proposed
  again.

`MarriageStatus` on the person is the cheap projection the client shows and the
eligibility check reads; the pair edge is the fact. The spouse is derived from the
edge wherever it is needed (a villager's chat briefing tells them who they are
married to), never stored twice.

Once the couple shares a completed home, the family-planning loop in
[families.md](families.md) becomes eligible. Their children take the household surname.

## The couple names itself

The married name is not a rule's to pick; it is the couple's. Once the brain has
blessed the marriage, it convenes the two betrothed and they settle the name
themselves, in a group chat run on the shared conversation engine
(`MarriageNaming`, on `Dialogue`; see [conversations.md](conversations.md)). This
is the "just ask the villager" pattern applied to a decision that is genuinely
theirs: they talk it over as themselves, in the first person and overheard by any
player nearby, and the way to end the talk is a valid choice.

The choice is constrained to what a marriage can sensibly make of two names: keep
one, take the other, or join them hyphenated in either order (`Ada Hollic` and
`Bren Vane` may become the `Hollic`, `Vane`, `Hollic-Vane`, or `Vane-Hollic`
household). The model is shown those exact options and its answer is validated
against them, so no invented surname can slip through.

The decision stays pending (`Village.marriageDecisionPending`) across the whole
talk, so no other marriage in the village starts while it runs. When the talk
lands no valid name (a quiet model, or a pair who never agree), the wedding falls
back to the hyphenation the two derive by UUID order, so the couple's voice is
honoured when they use it and a marriage is never blocked when they do not. This
is the same graceful-deferral contract every LLM path in the mod keeps.

## Housing queues, it does not preempt

A married pair who do not yet occupy the two beds of one declared couple room is a
**couple awaiting one**, derived each pass from married pairs and bed assignments, never
stored as its own list. Sharing a building while sleeping in different rooms does not satisfy
this need and does not enable family planning.

Completed homes are considered first, even while the village is saving for another project.
A house can contain single rooms alongside one or several couple rooms. Each couple moves into
one complete pair atomically; neither spouse moves if the other side is unavailable, and other
room occupants stay in place. Their former beds return to the existing bed pool. Taking a job
or changing workplaces does not pull a housed spouse into a separate single live-in bed. A free
staff couple room can instead move both spouses together when one claims its workplace job.

When no existing room can house a waiting couple and the village is **not already saving for
something else**, it names a suitable home as its `VillageGoal`. Styles with a dedicated
`couple_cottage` retain that choice; styles with mixed housing can save for a house containing a
pair instead. The smallest suitable mixed home is considered first. The same construction,
affordability and stalled-goal machinery applies. A marriage never replaces an unrelated goal.

Building definitions declare pairs by their authored bed coordinates:

```json
"beds": [[1,1,1], [2,1,1], [5,1,1]],
"couple_beds": [[[1,1,1], [2,1,1]]]
```

This is one couple room and one single bed. A pair has exactly two distinct adjacent beds on the
same floor; each coordinate must also occur in `beds`, and no bed belongs to two pairs. Pair
membership is resolved from coordinates against the current bed list, including after a reload
or rebuild. There is no separate saved household allocation. Existing cottages without
`couple_beds` retain their first two beds as a pair; ordinary houses infer no pair, even when two
beds happen to touch. An explicit empty list disables pair inference.

Both beds in a pair are reserved against unrelated single arrivals, job claims, and adulthood
claims. Singles in the same building remain available as ordinary housing. Death and travel
release beds through the existing ledger: a surviving occupant keeps their side, while a new
couple can claim that room only after both sides are free. The planning and chat briefings show
single-bed availability, worker beds, complete free couple rooms, and couples still awaiting one.

A workplace can reserve a couple room with `worker_beds` containing both bed coordinates.
At least one spouse must work in that building; the other can have any job or none. The same
housing gate lets a bedless married applicant claim that job and move both spouses into the free
pair. Unrelated couples cannot claim the reserved room, and neither occupant's current bed is
released merely because one side of a desired destination is available. A staff couple room
remains their home while either spouse works there; it releases both sides when neither does.
A spouse already in a valid staff room stays there even if their own job also offers a couple room.

Role-reserved rooms use the same allocator and ledger. The royal suite reserves its pair for
the castle's `LEADER` and their resident spouse; a single incumbent can use it without being
married. Other castle employees and unrelated couples cannot occupy either side. Losing the
ruling job releases the role entitlement for both spouses, and ordinary marriage housing finds
a replacement room. A guard captain with a spouse does not leave their shared home for the
castle's single captain bed. Moving into a reserved pair validates the entire destination
before either spouse's previous bed is released.

Returning spouses claim a pair only after both travelers have joined the resident roster.
Their restored marriage edge alone does not reserve beds while they are still walking in.

A couple shares a room chest by mapping both beds to that chest in `bed_containers`.
`PersonalChest` derives ownership from those existing mappings; other rooms in a mixed home keep
their own containers. Bedrooms without a personal container are also valid.

## The household travels together

Attractiveness-driven emigration never splits a resident nuclear family. The selected adult,
their resident spouse, and all dependent children leave together when the population floor can
spare the whole group. On the road they follow one household leader, and the horizon pool restores
the complete family when another village draws one member back. Their marriage and close-family
edges are rebuilt at the destination. See [families.md](families.md) and
[population-and-labor.md](population-and-labor.md).

## The couple's cottage

A new building category, `couple_cottage`. Unlike every other category it is
**never chosen spontaneously** by the brain: the planner filters it out of the
options it deliberates over (`UrbanPlanner.isMarriageOnly`), so it is built only as
the goal a marriage sets. A village should not raise one for no one.

The bundled cottage is the approved Birch one (`couple_cottage_birch_forest_1`, a 15x7x19
home from the Birch selection in [birch-village.md](birch-village.md)). Its one defining
feature is the **double bed**: two beds side by side in the primary village color, the
couple's shared sleeping nook, rather than the separate beds a shared house carries. Its beds
are named at the **foot** block, the coordinate the building definition's `beds` array
expects.

The first cottage, the hand-built plains one, was authored the way the repo's structures are
meant to be ([structure-authoring.md](structure-authoring.md)): built by hand in-world, then
captured with `/kkdev village save-structure`, and copied into `resources/`, with every
bottom-layer cell that is not floor or garden left as structure-void so placing it on real
ground lays its floor without carving a pit, and checked with `validate.py` (nothing drops
on placement) and `navcheck.py` (both beds reachable and on the ground floor, the door
reachable). It went with the old Village Life families on 2026-09-10, along with the taiga,
snowy, desert and savanna cottages that were derived from it.

The Birch cottage retains the dedicated-cottage housing behavior; new regional catalogs may
instead supply mixed houses with explicit couple rooms.

## Code map

- `relationships/MarriageService`: the pass, run on the village tick. Files
  proposals, asks the brain, weds, names the home goal, houses the couple on
  completion or when a completed room becomes available.
- `village/MarriageProposals`: the proposal store on the brain's `strategy` tag,
  with mutual-match and clear-on-wed.
- `relationships/RelationshipPair`: gained a `married` flag (additive codec field),
  preserved through drift; the pair edge is the marriage.
- `entities/RealPerson#marry`: the person-level projection, `MARRIED` plus the
  hyphenated surname, on both spouses at once.
- `village/Village`: the tick pass, `marriageDecisionPending`, `marriedPairs`, and
  `houseCouple` and `sharesCoupleHome`; completed mixed homes and cottages use the same hook.
- `village/CoupleHousing`: atomic room assignment over the existing bed ledger.
- `village/buildings/BuildingInfo#CoupleBeds`: authored bed pairs, without saved duplicate household state.
- `village/buildings/UrbanPlanner#isMarriageOnly`: keeps the dedicated cottage out of the
  brain's spontaneous options, and `shortfallFor` names the housing goal with a
  real shortfall so it stalls honestly.
- `chat/PersonChatContext#spouseLine`: a married villager's own knowledge of who
  they are wed to, derived from the edge.
- `src/main/resources/data/kithkyn/kithkyn/buildings/couple_cottage_birch_forest_1.json`
  and its `.nbt`; the approved Birch capture.
