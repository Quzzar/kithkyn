package com.quzzar.kithkyn.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

/** One saved store for a resident's matters, including server-owned action commitments and their actual lifecycle. */
public record UndertakingData(List<Undertaking> undertakings) {

    /** Open matters kept per villager; resolved ones trim oldest-first past this. */
    public static final int MAX_OPEN = 16;
    public static final int MAX_RESOLVED = 16;

    public UndertakingData { undertakings = List.copyOf(undertakings); }

    public static final UndertakingData EMPTY = new UndertakingData(List.of());

    /** How the villager feels about the matter, independent of whether it is done. */
    public enum Valence { POSITIVE, NEGATIVE }

    /** Where a matter is in its life. RESOLVED and ABANDONED are terminal. */
    public enum State { OPEN, ACTIVE, RESOLVED, ABANDONED }

    /** Who raised it: the villager themselves, the world, or the player's doing. */
    public enum Origin { SELF, EVENT, PLAYER }

    public enum Lane { PLAYER_REQUEST, REACTION }
    public enum Phase {
        REQUESTED, ACCEPTED, UNDERWAY, COMPLETED, FAILED, INTERRUPTED, CANCELLED, EXPIRED;
        public boolean executing() { return this == ACCEPTED || this == UNDERWAY; }
    }

    public record Position(double x, double y, double z) {
        public static final Codec<Position> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.DOUBLE.fieldOf("x").forGetter(Position::x), Codec.DOUBLE.fieldOf("y").forGetter(Position::y),
            Codec.DOUBLE.fieldOf("z").forGetter(Position::z)).apply(inst, Position::new));
        public boolean finite() { return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z); }
    }

    public record Duty(String occupation, String age, String village, Optional<UUID> workplace, int station,
            boolean essential, boolean fixedPost) {
        public static final Codec<Duty> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("occupation").forGetter(Duty::occupation), Codec.STRING.fieldOf("age").forGetter(Duty::age),
            Codec.STRING.fieldOf("village").forGetter(Duty::village), UUIDUtil.STRING_CODEC.optionalFieldOf("workplace").forGetter(Duty::workplace),
            Codec.INT.fieldOf("station").forGetter(Duty::station), Codec.BOOL.fieldOf("essential").forGetter(Duty::essential),
            Codec.BOOL.fieldOf("fixed_post").forGetter(Duty::fixedPost)).apply(inst, Duty::new));
    }

    /** Game-owned arguments. Dialogue cannot create, edit, or resolve an execution record. */
    public record Execution(UUID resident, Lane lane, String action, Phase phase, String requesterName,
            String dimension, Optional<Position> destination, Optional<UUID> reference, long revision,
            long startedAt, long expiresAt, boolean reconsidered, Duty duty) {
        public static final Codec<Execution> CODEC = RecordCodecBuilder.<Execution>create(inst -> inst.group(
            UUIDUtil.STRING_CODEC.fieldOf("resident").forGetter(Execution::resident),
            com.quzzar.kithkyn.utils.KithkynCodecs.forEnum(Lane.class).fieldOf("lane").forGetter(Execution::lane),
            Codec.STRING.fieldOf("action").forGetter(Execution::action),
            com.quzzar.kithkyn.utils.KithkynCodecs.forEnum(Phase.class).fieldOf("phase").forGetter(Execution::phase),
            Codec.STRING.fieldOf("requester_name").forGetter(Execution::requesterName),
            Codec.STRING.fieldOf("dimension").forGetter(Execution::dimension),
            Position.CODEC.optionalFieldOf("destination").forGetter(Execution::destination),
            UUIDUtil.STRING_CODEC.optionalFieldOf("reference").forGetter(Execution::reference),
            Codec.LONG.fieldOf("revision").forGetter(Execution::revision),
            Codec.LONG.fieldOf("started_at").forGetter(Execution::startedAt),
            Codec.LONG.fieldOf("expires_at").forGetter(Execution::expiresAt),
            Codec.BOOL.optionalFieldOf("reconsidered", false).forGetter(Execution::reconsidered),
            Duty.CODEC.fieldOf("duty").forGetter(Execution::duty)
        ).apply(inst, Execution::new)).validate(execution -> execution.valid()
            ? com.mojang.serialization.DataResult.success(execution)
            : com.mojang.serialization.DataResult.error(() -> "Invalid saved execution arguments"));
        public boolean valid() {
            var verbs = lane == Lane.PLAYER_REQUEST ? java.util.Set.of("follow", "wait", "go_to_point", "resume")
                : java.util.Set.of("continue", "warn", "retreat", "seek_help", "investigate", "defend");
            return verbs.contains(action) && !dimension.isBlank() && dimension.length() <= 128 && requesterName.length() <= 64
                && revision > 0 && startedAt >= 0 && expiresAt > startedAt && expiresAt - startedAt <= 6000
                && destination.map(Position::finite).orElse(true)
                && (!java.util.Set.of("wait", "go_to_point", "retreat", "seek_help", "investigate").contains(action) || destination.isPresent());
        }
        public Execution phase(Phase next) { return new Execution(resident, lane, action, next, requesterName,
            dimension, destination, reference, revision, startedAt, expiresAt, reconsidered, duty); }
        public Execution reconsider() { return new Execution(resident, lane, action, phase, requesterName,
            dimension, destination, reference, revision, startedAt, expiresAt, true, duty); }
    }

    /** One ordered progress marker on a milestoned undertaking. */
    public record Milestone(String text, boolean reached) {
        public static final Codec<Milestone> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("text").forGetter(Milestone::text),
                Codec.BOOL.optionalFieldOf("reached", false).forGetter(Milestone::reached)
        ).apply(inst, Milestone::new));
    }

    /**
     * @param id        stable handle, so an advance or resolve names the right matter
     * @param valence   how the villager feels about it
     * @param state     where it is in its life
     * @param origin    who raised it
     * @param summary   the villager's own one sentence
     * @param withWhom  the player or villager it concerns, if any
     * @param steps     recorded lifecycle markers, or milestones retained from earlier narrative matters
     * @param progressNote the latest factual execution status, or an earlier narrative note
     * @param resolution one sentence, filled only when it ends
     * @param openedDay  level day-time when it began
     * @param updatedDay level day-time it last moved
     */
    public record Undertaking(
            UUID id,
            Valence valence,
            State state,
            Origin origin,
            String summary,
            Optional<UUID> withWhom,
            List<Milestone> steps,
            Optional<String> progressNote,
            Optional<String> resolution,
            long openedDay,
            long updatedDay,
            Optional<Execution> execution) {

        public static final Codec<Undertaking> CODEC = RecordCodecBuilder.<Undertaking>create(inst -> inst.group(
                UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(Undertaking::id),
                com.quzzar.kithkyn.utils.KithkynCodecs.forEnum(Valence.class).fieldOf("valence").forGetter(Undertaking::valence),
                com.quzzar.kithkyn.utils.KithkynCodecs.forEnum(State.class).fieldOf("state").forGetter(Undertaking::state),
                com.quzzar.kithkyn.utils.KithkynCodecs.forEnum(Origin.class).fieldOf("origin").forGetter(Undertaking::origin),
                Codec.STRING.fieldOf("summary").forGetter(Undertaking::summary),
                UUIDUtil.STRING_CODEC.optionalFieldOf("with").forGetter(Undertaking::withWhom),
                Milestone.CODEC.listOf().optionalFieldOf("steps", List.of()).forGetter(Undertaking::steps),
                Codec.STRING.optionalFieldOf("progress_note").forGetter(Undertaking::progressNote),
                Codec.STRING.optionalFieldOf("resolution").forGetter(Undertaking::resolution),
                Codec.LONG.fieldOf("opened_day").forGetter(Undertaking::openedDay),
                Codec.LONG.fieldOf("updated_day").forGetter(Undertaking::updatedDay),
                Execution.CODEC.optionalFieldOf("execution").forGetter(Undertaking::execution)
        ).apply(inst, Undertaking::new)).validate(matter -> matter.execution().filter(execution -> execution.lane() == Lane.PLAYER_REQUEST && matter.withWhom().isEmpty()).isPresent()
            ? com.mojang.serialization.DataResult.error(() -> "Player undertaking is missing its requester")
            : com.mojang.serialization.DataResult.success(matter));

        public Undertaking { steps = List.copyOf(steps); }

        public boolean isOpen() {
            return state == State.OPEN || state == State.ACTIVE;
        }


    }

    public static final Codec<UndertakingData> CODEC = RecordCodecBuilder.<UndertakingData>create(inst -> inst.group(
            Undertaking.CODEC.listOf().fieldOf("undertakings").forGetter(UndertakingData::undertakings)
    ).apply(inst, UndertakingData::new)).validate(data -> data.undertakings().size() <= MAX_OPEN + MAX_RESOLVED
        && data.allOpen().size() <= MAX_OPEN && data.undertakings().stream().map(Undertaking::id).distinct().count() == data.undertakings().size()
        ? com.mojang.serialization.DataResult.success(data) : com.mojang.serialization.DataResult.error(() -> "Invalid undertaking bounds or duplicate IDs"));

    /** Everything the villager is currently seeing through, for "what's on your plate?". */
    public List<Undertaking> allOpen() {
        return undertakings.stream().filter(Undertaking::isOpen).toList();
    }

    public Optional<Undertaking> byId(UUID id) {
        return undertakings.stream().filter(u -> u.id().equals(id)).findFirst();
    }

    /** Replaces a matter without evicting open promises; closed history is capped. */
    public UndertakingData with(Undertaking added) {
        List<Undertaking> updated = new ArrayList<>(undertakings);
        updated.removeIf(u -> u.id().equals(added.id())); // replace in place on update
        updated.add(added);
        if (updated.stream().filter(Undertaking::isOpen).count() > MAX_OPEN) {
            throw new IllegalStateException("Open undertaking capacity must be checked before writing");
        }
        trimHistory(updated);
        return new UndertakingData(List.copyOf(updated));
    }

    /** Keeps bounded closed history, oldest dropped first. */
    private static void trimHistory(List<Undertaking> list) {
        int max = MAX_RESOLVED;
        List<Undertaking> kind = list.stream().filter(u -> !u.isOpen()).toList();
        if (kind.size() <= max) {
            return;
        }
        List<Undertaking> byAge = new ArrayList<>(kind);
        byAge.sort((a, b) -> Long.compare(a.updatedDay(), b.updatedDay()));
        for (int i = 0; i < byAge.size() - max; i++) {
            list.remove(byAge.get(i));
        }
    }
}
