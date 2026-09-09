package com.quzzar.kithkyn.utils;

import static org.junit.jupiter.api.Assertions.*;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import org.junit.jupiter.api.Test;

class ExactNbtTest {
  @Test
  void coordinatesKeepListTypesThroughRegistryOpsAndJson() {
    var tag = new CompoundTag();
    var xyz = new ListTag();
    xyz.add(IntTag.valueOf(10)); xyz.add(IntTag.valueOf(1)); xyz.add(IntTag.valueOf(10));
    tag.put("pos", xyz);
    tag.putIntArray("UUID", new int[]{1,2,3,4});
    assertRoundTrip(NbtOps.INSTANCE, tag);
    assertRoundTrip(RegistryOps.create(NbtOps.INSTANCE, HolderLookup.Provider.create(Stream.empty())), tag);
    assertRoundTrip(JsonOps.INSTANCE, tag);
  }

  @Test
  void malformedCompressedDataIsAParseError() {
    assertTrue(KithkynCodecs.EXACT_NBT.parse(NbtOps.INSTANCE, new ByteArrayTag(new byte[]{1,2,3})).error().isPresent());
  }

  private static <T> void assertRoundTrip(DynamicOps<T> ops, CompoundTag tag) {
    var restored = KithkynCodecs.EXACT_NBT.parse(ops, KithkynCodecs.EXACT_NBT.encodeStart(ops, tag).getOrThrow()).getOrThrow();
    assertEquals(tag, restored);
    assertEquals(3, restored.getList("pos", Tag.TAG_INT).size());
    assertInstanceOf(IntArrayTag.class, restored.get("UUID"));
  }
}
