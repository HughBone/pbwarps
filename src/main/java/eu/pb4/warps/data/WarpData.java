package eu.pb4.warps.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.WrappedText;
import eu.pb4.predicate.api.MinecraftPredicate;
import eu.pb4.predicate.api.PredicateContext;
import eu.pb4.predicate.api.PredicateRegistry;
import eu.pb4.warps.WarpAdmins;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;

public record WarpData(String id, int priority, WrappedText name, ItemStack icon, Target target,
                       Optional<MinecraftPredicate> predicate, Privacy privacy, Optional<UUID> owner) {
    public static final NodeParser NAME_PARSER = NodeParser.builder().requireSafe().legacyAll().quickText().build();

    public static final Codec<WarpData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(WarpData::id),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(WarpData::priority),
            NAME_PARSER.codec().fieldOf("name").forGetter(WarpData::name),
            ItemStack.OPTIONAL_CODEC.lenientOptionalFieldOf("icon", Items.GRASS_BLOCK.getDefaultInstance()).forGetter(WarpData::icon),
            Target.CODEC.forGetter(WarpData::target),
            PredicateRegistry.CODEC.lenientOptionalFieldOf("predicate").forGetter(WarpData::predicate),
            Privacy.CODEC.optionalFieldOf("privacy", Privacy.PUBLIC).forGetter(WarpData::privacy),
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(WarpData::owner)
    ).apply(instance, WarpData::new));

    public WarpData(String id, Target target) {
        this(id, target, null);
    }

    public WarpData(String id, Target target, @Nullable UUID owner) {
        this(id, 0, WrappedText.from(NAME_PARSER, id), Items.GRASS_BLOCK.getDefaultInstance(), target, Optional.empty(), Privacy.PUBLIC, Optional.ofNullable(owner));
    }

    public WarpData withId(String id) {
        return new WarpData(id, priority, name, icon, target, predicate, privacy, owner);
    }

    public WarpData withName(String name) {
        return new WarpData(id, priority, WrappedText.from(NAME_PARSER, name), icon, target, predicate, privacy, owner);
    }

    public WarpData withIcon(ItemStack icon) {
        return new WarpData(id, priority, name, icon, target, predicate, privacy, owner);
    }

    public WarpData withTarget(Target target) {
        return new WarpData(id, priority, name, icon, target, predicate, privacy, owner);
    }

    public WarpData withPredicate(@Nullable MinecraftPredicate predicate) {
        return new WarpData(id, priority, name, icon, target, Optional.ofNullable(predicate), privacy, owner);
    }
    public WarpData withPriority(int priority) {
        return new WarpData(id, priority, name, icon, target, predicate, privacy, owner);
    }

    public WarpData withPrivacy(Privacy privacy) {
        return new WarpData(id, priority, name, icon, target, predicate, privacy, owner);
    }

    public void handleTeleport(Entity entity) {
        var target = this.target().asTeleportTarget(Objects.requireNonNull(entity.level().getServer()), entity, this::teleportEffects);

        if (target != null) {
            if (!entity.isInvisible()) {
                entity.level().broadcastEntityEvent(entity, (byte) 46);
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            entity.teleport(target);
        }
    }

    private void teleportEffects(Entity entity) {
        if (!entity.isInvisible()) {
            entity.level().gameEvent(GameEvent.TELEPORT, entity.position(), GameEvent.Context.of(entity));
            entity.level().broadcastEntityEvent(entity, (byte) 46);
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public boolean canUse(CommandSourceStack source) {
        return this.isVisibleTo(source) && (this.predicate.isEmpty() || this.predicate.get().test(PredicateContext.of(source)).success());
    }

    /**
     * A private warp is only visible to (and usable by) its creator. Public warps are visible to everyone.
     */
    public boolean isVisibleTo(CommandSourceStack source) {
        if (this.privacy != Privacy.PRIVATE) {
            return true;
        }
        var entity = source.getEntity();
        if (entity == null) {
            return false;
        }
        // Warp admins can see any private warp, regardless of ownership.
        if (WarpAdmins.get().isAdmin(entity.getUUID())) {
            return true;
        }
        return this.owner.isPresent() && this.owner.get().equals(entity.getUUID());
    }
}
