import java.nio.file.*;
import net.minecraft.nbt.*;

/** Copies a stopped save, adding only the old template for each pending building's saved cursor. */
public final class FreezeBuildingProjects {
  public static void main(String[] args) throws Exception {
    Path input = Path.of(args[0]).toRealPath();
    Path output = Path.of(args[1]).toAbsolutePath();
    Path assets = Path.of(args[2]).toRealPath();
    if (input.equals(output) || Files.exists(output)) throw new IllegalArgumentException("Output must be a new copy");
    CompoundTag root = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
    CompoundTag before = root.copy();
    CompoundTag villages = root.getCompound("data").getCompound("Villages");
    int count = 0;
    for (String id : villages.getAllKeys()) {
      CompoundTag project = villages.getCompound(id).getCompound("project");
      if (!project.contains("building", Tag.TAG_COMPOUND)) continue;
      CompoundTag work = project.getCompound("building");
      if (work.contains("template_snapshot")) continue;
      String name = work.getCompound("building").getString("name");
      if (!name.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid building name");
      Path template = assets.resolve(name + ".nbt");
      NbtIo.readCompressed(template, NbtAccounter.unlimitedHeap());
      work.putByteArray("template_snapshot", Files.readAllBytes(template));
      count++;
    }
    CompoundTag stripped = root.copy();
    CompoundTag copiedVillages = stripped.getCompound("data").getCompound("Villages");
    for (String id : copiedVillages.getAllKeys()) {
      if (!before.getCompound("data").getCompound("Villages").getCompound(id).getCompound("project")
          .getCompound("building").contains("template_snapshot")) {
        copiedVillages.getCompound(id).getCompound("project").getCompound("building").remove("template_snapshot");
      }
    }
    if (!stripped.equals(before)) throw new IllegalStateException("Migration changed unrelated state");
    NbtIo.writeCompressed(root, output);
    if (!NbtIo.readCompressed(output, NbtAccounter.unlimitedHeap()).equals(root)) throw new IllegalStateException("Copy verification failed");
    System.out.println("Preserved all existing NBT exactly; added " + count + " project template snapshots to " + output);
  }
}
