package com.quzzar.kithkyn.dev;

import com.quzzar.kithkyn.Kithkyn;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.village.CompanionPets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Isolated diagnostic only: exercise the real join handler and vanilla sitting goal. */
@EventBusSubscriber(modid=Kithkyn.MODID)
public final class PetSittingProbe {
  private static int ticks;
  @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
    if (!Boolean.getBoolean("kithkyn.petSittingProbe") || ++ticks!=40) return;
    MinecraftServer server=event.getServer();
    try {
      var level=server.overworld();
      RealPerson owner=null;
      for (var entity:level.getAllEntities()) if (entity instanceof RealPerson person) {owner=person;break;}
      if(owner==null)throw new IllegalStateException("Fixture owner missing");
      owner.setNoAi(true);owner.moveTo(76.5,5,76.5,0,0);
      boolean failed=false;
      for(var type:java.util.List.of(EntityType.CAT,EntityType.WOLF)) {
        TamableAnimal pet=type.create(level);
        pet.moveTo(68.5,5,76.5,0,0);pet.setTame(true,true);pet.setOwnerUUID(owner.getUUID());
        pet.getPersistentData().putBoolean(CompanionPets.COMPANION_PET_KEY,true);
        pet.getPersistentData().putUUID(CompanionPets.PET_OWNER_KEY,owner.getUUID());
        level.addFreshEntity(pet);pet.setOnGround(true);
        CompanionPets.commandSit(pet,false);
        var sit=pet.goalSelector.getAvailableGoals().stream().filter(g->g.getGoal() instanceof SitWhenOrderedToGoal).findFirst().orElseThrow();
        if(Boolean.getBoolean("kithkyn.petSittingProbe.control")) {
          pet.goalSelector.removeGoal(sit.getGoal());
          pet.goalSelector.addGoal(sit.getPriority(),new SitWhenOrderedToGoal(pet) {
            @Override public boolean canUse() {return pet.isOrderedToSit() && super.canUse();}
          });
          sit=pet.goalSelector.getAvailableGoals().stream().filter(g->g.getGoal() instanceof SitWhenOrderedToGoal).findFirst().orElseThrow();
        }
        boolean eligible=sit.canUse();pet.goalSelector.tick();
        boolean bad=!pet.isOrderedToSit() && pet.isInSittingPose();failed|=bad;
        Kithkyn.LOGGER.info("[pet-sitting-probe] type={} commandSit={} vanillaOwnerNull={} companionOwnerFound={} vanillaSitPriority={} vanillaSitCanUse={} sittingPose={} verdict={}",type,pet.isOrderedToSit(),pet.getOwner()==null,CompanionPets.loadedOwner(pet)==owner,sit.getPriority(),eligible,pet.isInSittingPose(),bad?"FAIL_UNREQUESTED_SITTING":"PASS");
        if(Boolean.getBoolean("kithkyn.petSittingProbe.control")) {
          CompanionPets.commandSit(pet,true);pet.goalSelector.tick();
          boolean stays=pet.isOrderedToSit() && pet.isInSittingPose();
          CompanionPets.commandSit(pet,false);pet.goalSelector.tick();
          boolean recalls=!pet.isOrderedToSit() && !pet.isInSittingPose();
          boolean follows=pet.goalSelector.getAvailableGoals().stream().anyMatch(g->g.isRunning() && g.getGoal() instanceof com.quzzar.kithkyn.entities.ai.goals.PetFollowOwnerGoal);
          failed|=!stays || !recalls || !follows;
          Kithkyn.LOGGER.info("[pet-sitting-probe] CONTROL type={} explicitSitWorks={} recallClearsPose={} followingGoalRunning={}",type,stays,recalls,follows);
        }
        pet.discard();
      }
      Kithkyn.LOGGER.info("[pet-sitting-probe] RESULT {}",failed?"FAIL":"PASS");
    }catch(Exception error){Kithkyn.LOGGER.error("[pet-sitting-probe] SETUP_FAIL",error);}
    finally{server.halt(false);}
  }
}
