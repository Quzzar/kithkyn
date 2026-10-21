package com.quzzar.kithkyn.wrongdoing;

import java.util.ArrayList;
import java.util.List;

import com.quzzar.kithkyn.entities.RealPerson;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Who saw it (docs/relationships.md, decided on #64). The single gate every
 * offence goes through: nothing a village does about wrongdoing happens without
 * someone to have seen it.
 *
 * That is a rule about blame rather than about consequence. A villager murdered
 * with nobody watching is still dead and still leaves the roster; what does not
 * happen is anyone deciding it was your fault.
 */
public final class Witnesses {

  /** How far a villager notices something happening, in blocks. */
  public static final double SIGHT_RANGE = 16.0D;

  private Witnesses() {
  }

  /** Everyone awake, near enough, with a clear line to the spot. */
  public static List<RealPerson> around(ServerLevel level, Vec3 where) {
    return around(level, where, null);
  }

  /**
   * The same, ignoring one person — the victim of what happened. Someone being
   * hurt is not a witness to it for the village's purposes: blame is what the
   * SETTLEMENT knows, and a villager set upon alone in a wood, dead or alive,
   * leaves the village none the wiser about who did it.
   */
  public static List<RealPerson> around(ServerLevel level, Vec3 where, RealPerson except) {
    List<RealPerson> seen = new ArrayList<>();
    for (RealPerson person : level.getEntitiesOfClass(RealPerson.class,
        new AABB(where, where).inflate(SIGHT_RANGE))) {
      if (person == except || person.isSleeping() || !person.isAlive()) {
        continue;
      }
      if (canSee(level, person, where)) {
        seen.add(person);
      }
    }
    return seen;
  }

  /** Shared sight gate: awake, within sixteen blocks, loaded terrain and a clear line to the target. */
  public static boolean canSee(ServerLevel level, RealPerson person, Vec3 where) {
    Vec3 eyes = person.getEyePosition();
    if (person.isSleeping() || !person.isAlive() || !Double.isFinite(where.x) || !Double.isFinite(where.y)
        || !Double.isFinite(where.z) || eyes.distanceToSqr(where) > SIGHT_RANGE * SIGHT_RANGE) return false;
    BlockPos from = BlockPos.containing(eyes);
    BlockPos to = BlockPos.containing(where);
    // Check every chunk in the tiny ray rectangle; point samples can miss a short diagonal crossing.
    for (int x = Math.min(from.getX(), to.getX()) >> 4; x <= (Math.max(from.getX(), to.getX()) >> 4); x++) {
      for (int z = Math.min(from.getZ(), to.getZ()) >> 4; z <= (Math.max(from.getZ(), to.getZ()) >> 4); z++) {
        if (!level.hasChunkAt(new BlockPos(x << 4, from.getY(), z << 4))) return false;
      }
    }
    HitResult hit = level.clip(new ClipContext(eyes, where,
        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, person));
    // A wall between them stops at the wall; a clear line reaches the spot.
    return hit.getType() == HitResult.Type.MISS
        || hit instanceof net.minecraft.world.phys.BlockHitResult block
            && block.getBlockPos().equals(BlockPos.containing(where));
  }

}
