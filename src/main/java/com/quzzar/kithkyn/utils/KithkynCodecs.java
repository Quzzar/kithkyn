package com.quzzar.kithkyn.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public final class KithkynCodecs {

    /** Opaque native NBT: RegistryOps must not normalize coordinate ListTags into array tags. */
    public static final Codec<net.minecraft.nbt.CompoundTag> EXACT_NBT = Codec.BYTE_BUFFER.comapFlatMap(buffer -> {
        try {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.duplicate().get(bytes);
            return DataResult.success(net.minecraft.nbt.NbtIo.readCompressed(new java.io.ByteArrayInputStream(bytes),
                    net.minecraft.nbt.NbtAccounter.create(64L * 1024 * 1024)));
        } catch (java.io.IOException | RuntimeException error) {
            return DataResult.error(() -> "Invalid native NBT snapshot: " + error.getMessage());
        }
    }, tag -> {
        try {
            var output = new java.io.ByteArrayOutputStream();
            net.minecraft.nbt.NbtIo.writeCompressed(tag, output);
            return java.nio.ByteBuffer.wrap(output.toByteArray());
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    });

    private KithkynCodecs() {
    }

    /** Codec for any enum, serialized by its constant name. */
    public static <E extends Enum<E>> Codec<E> forEnum(Class<E> clazz) {
        return Codec.STRING.comapFlatMap(name -> {
            try {
                return DataResult.success(Enum.valueOf(clazz, name));
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "Unknown " + clazz.getSimpleName() + " value: " + name);
            }
        }, Enum::name);
    }
}
