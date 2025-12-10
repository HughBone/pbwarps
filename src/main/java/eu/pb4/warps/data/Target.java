package eu.pb4.warps.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record Target(ResourceKey<Level> world, Vec3 pos, Optional<Float> pitch, Optional<Float> yaw) {
    public static final MapCodec<Target> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("world").forGetter(Target::world),
            Vec3.CODEC.fieldOf("pos").forGetter(Target::pos),
            Codec.FLOAT.optionalFieldOf("pitch").forGetter(Target::pitch),
            Codec.FLOAT.optionalFieldOf("yaw").forGetter(Target::yaw)
    ).apply(instance, Target::new));

    @Nullable
    public TeleportTransition asTeleportTarget(MinecraftServer server, Entity entity, TeleportTransition.PostTeleportTransition transition) {
        var world = server.getLevel(this.world);
        if (world == null) {
            return null;
        }
        return new TeleportTransition(world, this.pos, Vec3.ZERO, yaw.orElse(entity.getYRot()), pitch.orElse(entity.getXRot()), transition);
    }
}
