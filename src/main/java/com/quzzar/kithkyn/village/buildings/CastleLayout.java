package com.quzzar.kithkyn.village.buildings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.quzzar.kithkyn.village.Village;
import java.util.List;
import net.minecraft.core.BlockPos;

/** Authored castle amenities share the building's origin and rotation. */
public record CastleLayout(BlockPos custodyCell, BlockPos releasePoint, List<BlockPos> patrolPoints,
    List<BlockPos> evidenceContainers) {
  public static final Codec<CastleLayout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      BlockPos.CODEC.fieldOf("custody_cell").forGetter(CastleLayout::custodyCell),
      BlockPos.CODEC.fieldOf("release_point").forGetter(CastleLayout::releasePoint),
      BlockPos.CODEC.listOf().optionalFieldOf("patrol_points", List.of()).forGetter(CastleLayout::patrolPoints),
      BlockPos.CODEC.listOf().fieldOf("evidence_containers").forGetter(CastleLayout::evidenceContainers)
  ).apply(instance, CastleLayout::new));

  public CastleLayout {
    patrolPoints = List.copyOf(patrolPoints);
    evidenceContainers = List.copyOf(evidenceContainers);
  }

  /** One standing or commissioned castle supplies the settlement's single ruling position. */
  public static boolean canStart(Village village, BuildingInfo info) {
    if (info.getCastleLayout() == null) return true;
    if (village.getBuildings().stream().anyMatch(building -> building.getInfo() != null && building.getInfo().getCastleLayout() != null)) return false;
    var project = village.getCurrentProject();
    return project == null || project.getBuilding().getInfo() == null
        || project.getBuilding().getInfo().getCastleLayout() == null;
  }
}
