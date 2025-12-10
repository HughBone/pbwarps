package eu.pb4.warps.mixins;

import com.mojang.serialization.MapCodec;
import eu.pb4.predicate.api.MinecraftPredicate;
import eu.pb4.predicate.api.PredicateRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.resources.Identifier;

@Mixin(value = PredicateRegistry.class, remap = false)
public interface PredicateRegistryAccessor {
    @Accessor(remap = false)
    static Map<Identifier, MapCodec<MinecraftPredicate>> getCODECS() {
        throw new UnsupportedOperationException();
    }
}
