package com.floweytf.fma.mixin.sodium;

import com.floweytf.fma.FMAClient;
import com.floweytf.fma.util.Spinlock;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.VertexSerializerRegistryImpl;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = VertexSerializerRegistryImpl.class, remap = false)
public abstract class VertexSerializerRegistryImplMixin {
    // it turns out that spinlocks are so much faster than r/w locks that the minor performance
    // pessimization is irrelevant.
    @Unique
    private final Spinlock fma$lock = new Spinlock();

    @Shadow
    private static VertexSerializer createSerializer(VertexFormatDescription svf, VertexFormatDescription dvf) {
        throw new AssertionError();
    }

    @Unique
    private long fma$createKey(VertexFormatDescription srcFormat, VertexFormatDescription dstFormat) {
        // https://en.wikipedia.org/wiki/Pairing_function#Cantor_pairing_function
        long k1 = srcFormat.id();
        long k2 = dstFormat.id();
        return (k1 + k2) * (k1 + k2 + 1) / 2 + k2;
    }

    @Unique
    private VertexSerializer[] fma$cache = new VertexSerializer[4096];

    /**
     * @author Flowey
     * @reason Use cantor pairing function
     */
    @Overwrite
    public VertexSerializer get(VertexFormatDescription srcFormat, VertexFormatDescription dstFormat) {
        long identifier = fma$createKey(srcFormat, dstFormat);
        VertexSerializer serializer = find(identifier);

        if (serializer == null) {
            serializer = create(identifier, srcFormat, dstFormat);
        }

        return serializer;
    }

    /**
     * @author Flowey
     * @reason Use cantor pairing function
     */
    @Overwrite
    private VertexSerializer create(long identifier, VertexFormatDescription srcFormat,
                                    VertexFormatDescription dstFormat) {
        fma$lock.lock();

        VertexSerializer serializer;
        try {
            if (identifier > fma$cache.length) {
                long newLen = Math.max(identifier + 1, fma$cache.length * 2L);
                VertexSerializer[] vs = new VertexSerializer[(int) newLen];
                System.arraycopy(fma$cache, 0, vs, 0, fma$cache.length);
                FMAClient.LOGGER.info("VertexSerializerRegistryImplMixin#create: new fma$cache size: {}", vs.length);
                fma$cache = vs;
            }

            if (fma$cache[(int) identifier] == null) {
                fma$cache[(int) identifier] = createSerializer(srcFormat, dstFormat);
            }

            serializer = fma$cache[(int) identifier];
        } finally {
            fma$lock.unlock();
        }

        return serializer;
    }

    /**
     * @author Flowey
     * @reason Use cantor pairing function
     */
    @Overwrite
    private VertexSerializer find(long identifier) {
        fma$lock.lock();

        VertexSerializer res;
        try {
            res = fma$cache[(int) identifier];
        } finally {
            fma$lock.unlock();
        }

        return res;
    }
}
