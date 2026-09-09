package com.quzzar.kithkyn.village.buildings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import org.junit.jupiter.api.Test;

class BuildingEntitiesTest {
  @Test
  void receiptsSurviveSaveAndUpgradeWithoutRestocking() {
    Building building = new Building("butchery_birch_1", Rotation.NONE);
    assertFalse(building.getInitialEntities().complete());
    building.setInitialEntities(BuildingEntities.State.PENDING.withSpawned(0).withSpawned(2).withSpawned(0));
    Building restored = Building.CODEC.parse(JsonOps.INSTANCE,
        Building.CODEC.encodeStart(JsonOps.INSTANCE, building).getOrThrow()).getOrThrow();
    assertEquals(building.getInitialEntities(), restored.getInitialEntities());
    assertEquals(2, restored.getInitialEntities().spawned().size());
    Building upgraded = Building.upgradeOf(restored, "butchery_birch_2", new BlockPos(10, 20, 30), Rotation.NONE);
    assertEquals(restored.getInitialEntities(), upgraded.getInitialEntities());
    assertEquals(restored.getUUID(), upgraded.getUUID());
  }

  @Test
  void legacyStandingAndUpgradingBuildingsStaySealedButFreshUnfinishedProjectsCanSpawn() {
    Building source = new Building("bakery_birch_1", Rotation.NONE);
    var old = Building.CODEC.encodeStart(JsonOps.INSTANCE, source).getOrThrow().getAsJsonObject();
    old.remove("initial_entities");
    Building standing = Building.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow();
    assertTrue(standing.getInitialEntities().complete());
    standing.resumeLegacyInitialEntities(false, ConstructionMode.UPGRADE);
    assertTrue(standing.getInitialEntities().complete());
    Building fresh = Building.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow();
    fresh.resumeLegacyInitialEntities(false, ConstructionMode.FRESH);
    assertFalse(fresh.getInitialEntities().complete());
    Building finished = Building.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow();
    finished.resumeLegacyInitialEntities(true, ConstructionMode.FRESH);
    assertTrue(finished.getInitialEntities().complete());
  }

  @Test
  void aFrameGetsWorldAttachmentCoordinatesBeforeLoadWithoutMutatingTheTemplate() {
    CompoundTag source = new CompoundTag();
    source.putString("id", "minecraft:item_frame");
    source.putInt("TileX", 1450);
    source.putInt("TileY", 202);
    source.putInt("TileZ", 79);
    source.putByte("Facing", (byte)3);
    source.putByte("ItemRotation", (byte)4);
    UUID id = UUID.randomUUID();
    BlockPos destination = new BlockPos(-101, 73, 251);
    var entry = new StructureTemplate.StructureEntityInfo(Vec3.atCenterOf(destination), destination, source);
    CompoundTag placed = BuildingEntities.positionedTag(entry, id);
    assertEquals(destination.getX(), placed.getInt("TileX"));
    assertEquals(destination.getY(), placed.getInt("TileY"));
    assertEquals(destination.getZ(), placed.getInt("TileZ"));
    assertEquals(id, placed.getUUID("UUID"));
    assertEquals(3, placed.getByte("Facing"));
    assertEquals(4, placed.getByte("ItemRotation"));
    assertEquals(1450, source.getInt("TileX"));
    assertFalse(source.contains("UUID"));
  }

  @Test
  void retryIdsAreStableButDifferentBuildingsAndEntriesDoNotCollide() {
    UUID building = UUID.randomUUID();
    assertEquals(BuildingEntities.entityId(building, 0), BuildingEntities.entityId(building, 0));
    assertNotEquals(BuildingEntities.entityId(building, 0), BuildingEntities.entityId(building, 1));
    assertNotEquals(BuildingEntities.entityId(building, 0), BuildingEntities.entityId(UUID.randomUUID(), 0));
  }
}
