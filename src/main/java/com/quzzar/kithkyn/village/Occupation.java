package com.quzzar.kithkyn.village;

public enum Occupation {
    /**
     * No post yet: a resident who belongs at the campfire until a job opens
     * (docs/population-and-labor.md). Idle is the state; Wanderer is the title
     * a player sees, "Wanderer of Emberstead" rather than "Idle of". Saved by
     * name like every constant here, and worlds saved under the earlier names
     * (IDLE, NITWIT) are not read: the rename came with a wipe, not a migration.
     */
    WANDERER,
    GUARD,
    BLACKSMITH,
    FARMER,
    CLERIC,
    LIBRARIAN,
    MERCHANT,
    LUMBERJACK,
    BUILDER,
    MINER,
    MASON,
    TANNER,
    HUNTER,
    FISHER,
    // New-category occupations (bakery, butchery, brewery, pasture, inn). Present so their
    // building defs load through forEnum. BAKER, BUTCHER, and HERDER have work loops in
    // RealPerson.registerGoals; BREWER and INNKEEPER still lack theirs (#47) and behave
    // like the other loop-less jobs: assigned, but producing nothing.
    BAKER,
    BUTCHER,
    BREWER,
    HERDER,
    INNKEEPER,
    QUARTERMASTER,
    LEADER,

    /**
     * Not a village post but a traveller: a merchant sent out to wander and
     * trade like Minecraft's wandering trader (config "Wandering merchant").
     * A wandering merchant never joins a roster and keeps no bed; it trades
     * from a virtual snapshot of its home village's economy rather than that
     * village's chests, so it is exempt from the resident-only machinery.
     * Added, never renamed: saved by name like every constant here.
     */
    WANDERING_MERCHANT;

    public boolean isIdle() {
        return this == WANDERER;
    }

}
