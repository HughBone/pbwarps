package eu.pb4.warps;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import eu.pb4.predicate.api.PredicateContext;
import eu.pb4.predicate.api.PredicateRegistry;
import eu.pb4.warps.data.Privacy;
import eu.pb4.warps.data.Target;
import eu.pb4.warps.data.WarpData;
import eu.pb4.warps.mixins.PredicateRegistryAccessor;
import eu.pb4.warps.ui.WarpSelectGui;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.util.RandomSource;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static eu.pb4.warps.ModInit.id;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class WarpCommands {
    private static final SuggestionProvider<CommandSourceStack> WARP_ID_SUGGESTION_WITH_PREDICATE = (context, builder) -> {
        var ctx = PredicateContext.of(context.getSource());
        for (var warp : WarpManager.get().warps()) {
            if (warp.id().startsWith(builder.getRemainingLowerCase()) && warp.isVisibleTo(context.getSource()) && (warp.predicate().isEmpty() || warp.predicate().get().test(ctx).success())) {
                builder.suggest(warp.id(), warp.name().text());
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> WARP_ID_SUGGESTION = (context, builder) -> {
        for (var warp : WarpManager.get().warps()) {
            if (warp.id().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(warp.id().contains(" ") ? '"' + warp.id() + '"' : warp.id(), warp.name().text());
            }
        }
        return builder.buildFuture();
    };

    public static void init(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access, Commands.CommandSelection env) {
        dispatcher.register(literal("warp")
                .requires(FabricPermissionBridge.require(id("command"), true))
                .executes(WarpCommands::openWarpUi)
                .then(argument("id", StringArgumentType.greedyString()).suggests(WARP_ID_SUGGESTION_WITH_PREDICATE)
                        .executes(WarpCommands::warpTeleportSelf)
                )
        );

        Supplier<RequiredArgumentBuilder<CommandSourceStack, ?>> createPosition = () -> argument("position", Vec3Argument.vec3(true))
                .executes(WarpCommands::createWarp)
                .then(argument("rotation", RotationArgument.rotation())
                        .executes(WarpCommands::createWarp)
                        .then(argument("world", DimensionArgument.dimension())
                                .executes(WarpCommands::createWarp)
                        )
                );

        dispatcher.register(literal("warps")
                .requires(FabricPermissionBridge.require(id("warps_command"), true))
                .then(literal("create")
                        .requires(FabricPermissionBridge.require(id("create"), PermissionLevel.GAMEMASTERS))
                        .then(argument("id", StringArgumentType.string())
                                .executes(WarpCommands::createWarp)
                                .then(argument("name", StringArgumentType.string())
                                        .executes(WarpCommands::createWarp)
                                        .then(createPosition.get())
                                        .then(
                                                argument("icon", ItemArgument.item(access))
                                                        .executes(WarpCommands::createWarp)
                                                        .then(createPosition.get())
                                        )
                                )
                                .then(createPosition.get())
                        )
                )
                .then(literal("modify")
                        .requires(FabricPermissionBridge.require(id("modify"), PermissionLevel.GAMEMASTERS))
                        .then(argument("id", StringArgumentType.string())
                                .suggests(WARP_ID_SUGGESTION)
                                .then(literal("name")
                                        .requires(FabricPermissionBridge.require(id("modify/name"), PermissionLevel.GAMEMASTERS))
                                        .then(argument("name", StringArgumentType.greedyString()).executes(WarpCommands::setName))
                                )
                                .then(literal("position")
                                        .requires(FabricPermissionBridge.require(id("modify/position"), PermissionLevel.GAMEMASTERS))
                                        .executes(WarpCommands::setPosition)
                                        .then(argument("position", Vec3Argument.vec3(true))
                                                .executes(WarpCommands::setPosition)
                                                .then(argument("rotation", RotationArgument.rotation())
                                                        .executes(WarpCommands::setPosition)
                                                        .then(argument("world", DimensionArgument.dimension())
                                                                .executes(WarpCommands::setPosition)
                                                        )
                                                )
                                        )
                                )
                                .then(literal("icon")
                                        .requires(FabricPermissionBridge.require(id("modify/icon"), PermissionLevel.GAMEMASTERS))
                                        .then(argument("icon", ItemArgument.item(access)).executes(WarpCommands::setIcon))
                                )
                                .then(literal("priority")
                                        .requires(FabricPermissionBridge.require(id("modify/priority"), PermissionLevel.GAMEMASTERS))
                                        .then(argument("priority", IntegerArgumentType.integer()).executes(WarpCommands::setPriority))
                                )
                                .then(literal("privacy")
                                        .requires(FabricPermissionBridge.require(id("modify/privacy"), PermissionLevel.GAMEMASTERS))
                                        .then(literal("public").executes(context -> setPrivacy(context, Privacy.PUBLIC)))
                                        .then(literal("private").executes(context -> setPrivacy(context, Privacy.PRIVATE)))
                                )
                                .then(literal("predicate")
                                        .requires(FabricPermissionBridge.require(id("modify/predicate"), PermissionLevel.GAMEMASTERS))
                                        .then(literal("clear").executes(WarpCommands::clearPredicate))
                                        .then(argument("predicate_type", IdentifierArgument.id())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(PredicateRegistryAccessor.getCODECS().keySet(), builder))
                                                .executes(WarpCommands::setPredicate)
                                                .then(argument("data", CompoundTagArgument.compoundTag())
                                                        .executes(WarpCommands::setPredicate)
                                                )
                                        )
                                )

                        )
                )
                .then(literal("remove")
                        .requires(FabricPermissionBridge.require(id("remove"), PermissionLevel.GAMEMASTERS))
                        .then(argument("id", StringArgumentType.string())
                                .suggests(WARP_ID_SUGGESTION)
                                .executes(WarpCommands::removeWarp)
                        )
                )
                .then(literal("teleport")
                        .requires(WarpCommands::isWarpAdmin)
                        .then(argument("id", StringArgumentType.string())
                                .suggests(WARP_ID_SUGGESTION)
                                .executes(WarpCommands::warpTeleportSelfUnrestricted)
                                .then(argument("entity", EntityArgument.entities())
                                        .requires(FabricPermissionBridge.require(id("teleport/others"), PermissionLevel.GAMEMASTERS))
                                        .executes(WarpCommands::warpTeleportOthers)
                                )
                        )
                )
                .then(literal("info")
                        .requires(FabricPermissionBridge.require(id("info"), PermissionLevel.GAMEMASTERS))
                        .then(argument("id", StringArgumentType.string())
                                .suggests(WARP_ID_SUGGESTION)
                                .executes(WarpCommands::showInfo)
                        )

                )
                .then(literal("setowner")
                        .requires(WarpCommands::isWarpAdmin)
                        .then(argument("id", StringArgumentType.string())
                                .suggests(WARP_ID_SUGGESTION)
                                .then(argument("player", GameProfileArgument.gameProfile())
                                        .executes(WarpCommands::setOwner)
                                )
                        )
                )
        );

        // Restrict the vanilla /tp and /teleport commands to warp admins only.
        restrictToWarpAdmins(dispatcher, "teleport");
        restrictToWarpAdmins(dispatcher, "tp");
    }

    @SuppressWarnings("unchecked")
    private static void restrictToWarpAdmins(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        var node = dispatcher.getRoot().getChild(name);
        if (node != null) {
            ((eu.pb4.warps.mixins.CommandNodeAccessor<CommandSourceStack>) node).setRequirement(WarpCommands::isWarpAdmin);
        }
    }

    private static boolean isWarpAdmin(CommandSourceStack source) {
        var entity = source.getEntity();
        return entity != null && WarpAdmins.get().isAdmin(entity.getUUID());
    }

    private static int setOwner(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = StringArgumentType.getString(context, "id");
        var profiles = GameProfileArgument.getGameProfiles(context, "player");
        if (profiles.size() != 1) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.modify.owner.single_player").withStyle(ChatFormatting.RED));
            return 0;
        }
        var profile = profiles.iterator().next();

        if (WarpManager.get().updateWarp(id, x -> x.withOwner(profile.id()))) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.modify.owner", id, profile.name()));
            return 1;
        }

        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int showInfo(CommandContext<CommandSourceStack> context) {
        var id = StringArgumentType.getString(context, "id");
        var warp = WarpManager.get().get(id);
        if (warp == null) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
            return 0;
        }
        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.info.id", warp.id()));
        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.info.priority", warp.priority()));
        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.info.name", warp.name().text()));
        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.info.unformatted_name", warp.name().input()));
        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.info.icon", warp.icon().getDisplayName()));
        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.info.position", warp.target().pos().toString(), warp.target().yaw().map(String::valueOf).orElse("~"), warp.target().pitch().map(String::valueOf).orElse("~"), warp.target().world().identifier().toString()));
        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.info.privacy", warp.privacy().getSerializedName()));
        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.info.owner", warp.owner().map(UUID::toString).orElse("-")));
        if (warp.predicate().isPresent()) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.info.predicate_type", warp.predicate().get().identifier().toString()));
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.info.predicate_data", NbtUtils.toPrettyComponent(
                    warp.predicate().get().codec().codec().encodeStart(context.getSource().registryAccess().createSerializationContext(NbtOps.INSTANCE), warp.predicate().get()).getOrThrow()
            )));
        }
        return 1;
    }

    private static int clearPredicate(CommandContext<CommandSourceStack> context) {
        var id = StringArgumentType.getString(context, "id");
        if (WarpManager.get().updateWarp(id, x -> x.withPredicate(null))) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.modify.predicate.clear", id));
            return 1;
        }

        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int setPredicate(CommandContext<CommandSourceStack> context) {
        var id = StringArgumentType.getString(context, "id");
        var type = IdentifierArgument.getId(context, "predicate_type");
        var codec = PredicateRegistry.getCodec(type);
        if (codec == null) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.modify.predicate.invalid_predicate", type.toString()).withStyle(ChatFormatting.RED));
            return 0;
        }
        CompoundTag data = new CompoundTag();

        try {
            data = CompoundTagArgument.getCompoundTag(context, "data");
        } catch (Throwable ignored) {
        }

        var predicate = codec.codec().decode(context.getSource().registryAccess().createSerializationContext(NbtOps.INSTANCE), data);
        if (predicate.isError()) {
            var alt = new CompoundTag();
            alt.put("value", data);
            var maybe = codec.codec().decode(context.getSource().registryAccess().createSerializationContext(NbtOps.INSTANCE), alt);
            if (maybe.isSuccess()) {
                predicate = maybe;
            }
        }

        if (predicate.error().isPresent()) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.modify.predicate.invalid_predicate_data", type.toString()).withStyle(ChatFormatting.RED));
            context.getSource().sendSystemMessage(Component.literal(predicate.error().get().message()).withStyle(ChatFormatting.RED));
            return 0;
        }

        var finalPredicate = predicate;
        if (WarpManager.get().updateWarp(id, x -> x.withPredicate(finalPredicate.result().get().getFirst()))) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.modify.predicate", id, type.toString(), NbtUtils.toPrettyComponent(data)));
            return 1;
        }

        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int removeWarp(CommandContext<CommandSourceStack> context) {
        var id = StringArgumentType.getString(context, "id");

        if (WarpManager.get().removeWarp(id)) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.remove", id));
            return 1;
        }

        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int setIcon(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = StringArgumentType.getString(context, "id");
        var icon = ItemArgument.getItem(context, "icon").createItemStack(1);

        if (WarpManager.get().updateWarp(id, x -> x.withIcon(icon))) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.modify.icon", id, icon.getDisplayName()));
            return 1;
        }

        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int setPosition(CommandContext<CommandSourceStack> context) {
        var id = StringArgumentType.getString(context, "id");
        var target = getTarget(context);

        if (WarpManager.get().updateWarp(id, x -> x.withTarget(target))) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.modify.position", id, target.pos().toString(), target.yaw().map(String::valueOf).orElse("~"), target.pitch().map(String::valueOf).orElse("~"), target.world().identifier().toString()));
            return 1;
        }

        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int setName(CommandContext<CommandSourceStack> context) {
        var id = StringArgumentType.getString(context, "id");
        var name = StringArgumentType.getString(context, "name");

        if (WarpManager.get().updateWarp(id, x -> x.withName(name))) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.modify.name", id, Objects.requireNonNull(WarpManager.get().get(id)).name().text()));
            return 1;
        }

        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int setPrivacy(CommandContext<CommandSourceStack> context, Privacy privacy) {
        var id = StringArgumentType.getString(context, "id");
        var warp = WarpManager.get().get(id);
        if (warp == null) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
            return 0;
        }

        // The creator of the warp, or a warp admin, is allowed to change its privacy.
        var entity = context.getSource().getEntity();
        var isOwner = entity != null && warp.owner().isPresent() && warp.owner().get().equals(entity.getUUID());
        var isAdmin = entity != null && WarpAdmins.get().isAdmin(entity.getUUID());
        if (!isOwner && !isAdmin) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.modify.privacy.not_owner", id).withStyle(ChatFormatting.RED));
            return 0;
        }

        if (WarpManager.get().updateWarp(id, x -> x.withPrivacy(privacy))) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.modify.privacy", id, privacy.getSerializedName()));
            return 1;
        }

        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int setPriority(CommandContext<CommandSourceStack> context) {
        var id = StringArgumentType.getString(context, "id");
        var priority = IntegerArgumentType.getInteger(context, "priority");

        if (WarpManager.get().updateWarp(id, x -> x.withPriority(priority))) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.modify.priority", id, Objects.requireNonNull(WarpManager.get().get(id)).name().text()));
            return 1;
        }

        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int createWarp(CommandContext<CommandSourceStack> context) {
        var id = StringArgumentType.getString(context, "id");
        var target = getTarget(context);

        var icon = BuiltInRegistries.ITEM.getRandom(RandomSource.create()).orElseThrow().value().getDefaultInstance();

        var creator = context.getSource().getEntity();
        var warp = new WarpData(id, target, creator != null ? creator.getUUID() : null);

        try {
            icon = ItemArgument.getItem(context, "icon").createItemStack(1);
        } catch (Throwable ignored) {}
        warp = warp.withIcon(icon);


        try {
            warp = warp.withName(StringArgumentType.getString(context, "name"));
        } catch (Throwable ignored) {}

        if (WarpManager.get().addWarp(warp)) {
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.create.success", id, target.pos().toString(), target.yaw().map(String::valueOf).orElse("~"), target.pitch().map(String::valueOf).orElse("~"), target.world().identifier().toString()));
            return 1;
        }

        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.create.duplicate", id).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static Target getTarget(CommandContext<CommandSourceStack> context) {
        var world = context.getSource().getLevel().dimension();
        var pos = context.getSource().getPosition();
        float pitch = context.getSource().getRotation().x;
        float yaw = context.getSource().getRotation().y;

        try {
            pos = Vec3Argument.getVec3(context, "position");
        } catch (Throwable ignored) {
        }
        try {
            var vec = RotationArgument.getRotation(context, "rotation").getRotation(context.getSource());
            pitch = vec.x;
            yaw = vec.y;
        } catch (Throwable ignored) {
        }
        try {
            world = DimensionArgument.getDimension(context, "world").dimension();
        } catch (Throwable ignored) {
        }

        return new Target(world, pos, Optional.of(pitch), Optional.of(yaw));
    }

    private static int warpTeleportSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = StringArgumentType.getString(context, "id");
        var warp = WarpManager.get().get(id);
        if (warp != null && warp.canUse(context.getSource())) {
            warp.handleTeleport(context.getSource().getEntityOrException());
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.warp", warp.name().text()));
            return 1;
        }
        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int warpTeleportSelfUnrestricted(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = StringArgumentType.getString(context, "id");
        var warp = WarpManager.get().get(id);
        if (warp != null) {
            warp.handleTeleport(context.getSource().getEntityOrException());
            context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.warp", warp.name().text()));
            return 1;
        }
        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int warpTeleportOthers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = StringArgumentType.getString(context, "id");
        var entities = EntityArgument.getEntities(context, "entity");
        var warp = WarpManager.get().get(id);
        if (warp != null) {
            for (var entity : entities) {
                warp.handleTeleport(entity);
                context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.teleport.other", entity.getName(), warp.name().text()));
            }
            return 1;
        }
        context.getSource().sendSystemMessage(Component.translatable("command.pbwarps.invalid_warp", id).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int openWarpUi(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        WarpSelectGui.open(context.getSource().getPlayerOrException());
        return 0;
    }
}
