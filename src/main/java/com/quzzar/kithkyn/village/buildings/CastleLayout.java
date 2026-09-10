package com.quzzar.kithkyn.village.buildings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.quzzar.kithkyn.utils.KithkynCodecs;
import com.quzzar.kithkyn.village.GuardRole;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.quzzar.kithkyn.village.Village;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;

/** Authored castle amenities share the building's origin and rotation. */
public record CastleLayout(BlockPos custodyCell, BlockPos releasePoint, Map<GuardRole, List<BlockPos>> patrolRoutes,
    List<BlockPos> evidenceContainers) {
  private static final Codec<Map<GuardRole, List<BlockPos>>> ROUTES = Codec.unboundedMap(
      KithkynCodecs.forEnum(GuardRole.class), BlockPos.CODEC.listOf()).validate(routes -> {
        for (var entry : routes.entrySet()) {
          if (entry.getKey() != GuardRole.CROSSBOW_POST && entry.getKey() != GuardRole.SWORD_POST) {
            return DataResult.error(() -> "Castle patrol routes require CROSSBOW_POST or SWORD_POST");
          }
          if (new HashSet<>(entry.getValue()).size() < 2) {
            return DataResult.error(() -> "Castle patrol routes require at least two distinct waypoints");
          }
        }
        return DataResult.success(routes);
      });

  public static final Codec<CastleLayout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      BlockPos.CODEC.fieldOf("custody_cell").forGetter(CastleLayout::custodyCell),
      BlockPos.CODEC.fieldOf("release_point").forGetter(CastleLayout::releasePoint),
      ROUTES.optionalFieldOf("patrol_routes", Map.of()).forGetter(CastleLayout::patrolRoutes),
      BlockPos.CODEC.listOf().fieldOf("evidence_containers").forGetter(CastleLayout::evidenceContainers)
  ).apply(instance, CastleLayout::new));

  public CastleLayout {
    patrolRoutes = patrolRoutes.entrySet().stream().collect(Collectors.toUnmodifiableMap(
        Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    evidenceContainers = List.copyOf(evidenceContainers);
  }

  /** Routes describe movement only; the guard's existing duty still selects their equipment. */
  public List<BlockPos> patrolRoute(GuardRole role) {
    return role == null ? List.of() : patrolRoutes.getOrDefault(role, List.of());
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
