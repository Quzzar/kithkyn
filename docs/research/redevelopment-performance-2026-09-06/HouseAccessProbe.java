package com.quzzar.kithkyn.dev;
import com.quzzar.kithkyn.*;
import com.quzzar.kithkyn.entities.RealPerson;
import com.quzzar.kithkyn.entities.ai.goals.work.*;
import com.quzzar.kithkyn.village.*;
import com.quzzar.kithkyn.village.buildings.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
@EventBusSubscriber(modid=Kithkyn.MODID)
public final class HouseAccessProbe {
 static boolean reloaded;static boolean sawDemolition;static int ticks;static RealPerson builder;static Village village;static StructureInProgress project;
 static final String MODE=System.getProperty("kithkyn.accessProbe", "");
 @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
  if(MODE.isEmpty())return;
  MinecraftServer server=event.getServer();var level=server.overworld();
  try {
   ticks++;
   if(ticks==80){
    level.setDayTime(1000);
    village=VillageManager.get(level).getVillages().values().iterator().next();
    var id=village.getJobAssignmentsView().entrySet().stream().filter(e->e.getValue().getOccupation()==Occupation.BUILDER).findFirst().orElseThrow().getKey();
    builder=(RealPerson)level.getEntity(id);
    if(builder==null)throw new IllegalStateException("Saved builder is not loaded");
    for(var entity:level.getAllEntities())if(entity instanceof Mob mob && mob!=builder)mob.setNoAi(true);
    Building source=village.getBuildings().stream().filter(b->b.getName().equals("house_plains_1")).findFirst().orElseThrow();
    BlockPos ground=BlockPos.of(source.getOriginLocation()).above(source.getInfo().getSink()).offset(0,0,-2);
    var plan=RedevelopmentPlanner.assess(village,Buildings.getByName("house_plains_2"),source,ground,source.getRotation()).plan().orElseThrow();
    village.cancelGatheringProject("isolated access reproduction");
    if(!village.startRedevelopment(new ConstructionChoice(Buildings.getByName("house_plains_2"),plan.mode(),plan)))throw new IllegalStateException("Cannot start prepared project");
    project=village.getCurrentProject();builder.personMainInv.clearContent();
    var stacks=project.requiredMaterials();int slot=0;
    for(var stack:stacks)while(!stack.isEmpty())builder.personMainInv.setItem(slot++,stack.split(Math.min(stack.getCount(),stack.getMaxStackSize())));
    builder.teleportTo(127.5,5,131.5);builder.getNavigation().stop();
    var field=Mob.class.getDeclaredField("goalSelector");field.setAccessible(true);GoalSelector goals=(GoalSelector)field.get(builder);
    var stepField=WorkLoopGoal.class.getDeclaredField("step");stepField.setAccessible(true);
    goals.removeAllGoals(goal->{try{return !(goal instanceof OpenDoorGoal) && !(goal instanceof WorkLoopGoal<?> && (stepField.get(goal) instanceof GatherStep || stepField.get(goal) instanceof BuildStep));}catch(Exception e){throw new RuntimeException(e);}});
    if(MODE.equals("entrance-step"))level.setBlock(new BlockPos(128,5,131),Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,net.minecraft.core.Direction.EAST),3);
    if(MODE.equals("clear-overhead"))for(int x=128;x<=129;x++)for(int y=8;y<=9;y++)level.setBlock(new BlockPos(x,y,131),Blocks.AIR.defaultBlockState(),3);
    if(MODE.equals("open-door")) {for(int y=6;y<=7;y++){BlockPos pos=new BlockPos(129,y,131);var state=level.getBlockState(pos);level.setBlock(pos,state.setValue(DoorBlock.OPEN,true),3);}}
    Kithkyn.LOGGER.info("[house-access] START mode={} person={} width={} height={} step={} target={} door={} pack={}",MODE,builder.getFullName(),builder.getBbWidth(),builder.getBbHeight(),builder.maxUpStep(),BlockPos.of(project.getBuilding().getCenterLocation()),level.getBlockState(new BlockPos(129,6,131)),Materials.tally(builder.personMainInv));
    server.tickRateManager().requestGameToSprint(24000);
   }
   if(builder!=null && ticks%400==0){var path=builder.getNavigation().getPath();Kithkyn.LOGGER.info("[house-access] tick={} pos={} velocity={} horizontalCollision={} verticalCollision={} onGround={} door={} progress={} pathIndex={} pathCount={}",ticks-80,builder.position(),builder.getDeltaMovement(),builder.horizontalCollision,builder.verticalCollision,builder.onGround(),level.getBlockState(new BlockPos(129,6,131)),project.getProgress(),path==null?-1:path.getNextNodeIndex(),path==null?-1:path.getNodeCount());}
   if(builder!=null && project.getProgress()==BuildProgress.DEMOLISHING) {
    sawDemolition=true;
    if(MODE.startsWith("reload") && !reloaded && project.getRedevelopment().remainingBlocks()<project.getRedevelopment().plan().blocks().size()/2) {
     var saved=Village.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,village).getOrThrow();
     village=Village.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,saved).getOrThrow();village.attach(level);
     VillageManager.get(level).getVillages().put(village.getID(),village);project=village.getCurrentProject();
     builder.getNavigation().stop();
     var field=Mob.class.getDeclaredField("goalSelector");field.setAccessible(true);((GoalSelector)field.get(builder)).getAvailableGoals().forEach(goal->goal.stop());
     reloaded=true;Kithkyn.LOGGER.info("[house-access] RELOADED remaining={}",project.getRedevelopment().remainingBlocks());
    }
   }
   if(builder!=null && (project.getProgress()==BuildProgress.COMPLETE || ticks>=24080)){
    Kithkyn.LOGGER.info("[house-access] RESULT {} mode={} ticks={} progress={} position={}",project.getProgress()==BuildProgress.COMPLETE && sawDemolition && (!MODE.startsWith("reload") || reloaded)?"PASS":"FAIL",MODE,ticks-80,project.getProgress(),builder.position());server.tickRateManager().stopSprinting();server.halt(false);builder=null;
   }
  }catch(Exception e){Kithkyn.LOGGER.error("[house-access] SETUP_FAIL",e);server.halt(false);builder=null;}
 }
}
