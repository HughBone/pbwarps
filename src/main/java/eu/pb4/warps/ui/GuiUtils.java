package eu.pb4.warps.ui;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.warps.ModInit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public class GuiUtils {
    public static final Style TEXTURE_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.parse("pbwarps:gui"))).withColor(ChatFormatting.WHITE);

    public static final GuiElement EMPTY = GuiElement.EMPTY;
    public static final GuiElement EMPTY_STACK = new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
            .setName(Component.empty())
            .model(Identifier.parse("air"))
            .hideTooltip().build();
    public static final GuiElement FILLER = Util.make(() -> new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE)
                .setName(Component.empty())
                .hideTooltip().build());
    private static final Identifier BACK_TEXTURE = requestModel("back");
    private static final Identifier NEXT_PAGE_TEXTURE = requestModel("next_page");
    private static final Identifier PREVIOUS_PAGE_TEXTURE = requestModel("previous_page");

    private static Identifier requestModel(String back) {
        return ModInit.id("sgui/elements/" + back);
    }

    public static void register() {
    }

    public static GuiElementBuilder page(ServerPlayer player, int current, int max) {
        return (new GuiElementBuilder(Items.BOOK)).noDefaults().setName(
                Component.translatable("text.polydex.view.pages",
                        Component.literal("" + current).withStyle(ChatFormatting.WHITE),
                        Component.literal("" + max).withStyle(ChatFormatting.WHITE)
                ).withStyle(ChatFormatting.AQUA)
        );
    }

    public static GuiElement backButton(ServerPlayer player, Runnable callback, boolean back) {
        return backBase(player)
                .setName(Component.translatable(back ? "gui.back" : "text.pbwarps.close").withStyle(ChatFormatting.RED))
                .noDefaults()
                .hideDefaultTooltip()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    callback.run();
                }).build();
    }

    private static GuiElementBuilder backBase(ServerPlayer player) {
        return hasTexture(player) ?
                new GuiElementBuilder(Items.TRIAL_KEY).noDefaults().model(BACK_TEXTURE)
                : new GuiElementBuilder(Items.STRUCTURE_VOID).noDefaults();
    }

    public static final void playClickSound(ServerPlayer player) {
        player.connection.send(new ClientboundSoundEntityPacket(
                SoundEvents.UI_BUTTON_CLICK, SoundSource.UI, player, 0.5f, 1, player.getRandom().nextLong()
        ));
    }

    public static GuiElement nextPage(ServerPlayer player, PageAware gui) {
        return nextPageBase(player)
                .setName(Component.translatable("spectatorMenu.next_page").withStyle(ChatFormatting.WHITE))
                .noDefaults()
                .hideDefaultTooltip()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    gui.nextPage();
                }).build();
    }

    private static GuiElementBuilder nextPageBase(ServerPlayer player) {
        return hasTexture(player)
                ? new GuiElementBuilder(Items.TRIAL_KEY).noDefaults().model(NEXT_PAGE_TEXTURE)
                : new GuiElementBuilder(Items.PLAYER_HEAD).noDefaults().setProfileSkinTexture(GuiHeadTextures.GUI_NEXT_PAGE);
    }

    private static GuiElementBuilder previousPageBase(ServerPlayer player) {
        return hasTexture(player)
                ? new GuiElementBuilder(Items.TRIAL_KEY).noDefaults().model(PREVIOUS_PAGE_TEXTURE)
                : new GuiElementBuilder(Items.PLAYER_HEAD).noDefaults().setProfileSkinTexture(GuiHeadTextures.GUI_PREVIOUS_PAGE);
    }

    public static GuiElement previousPage(ServerPlayer player, PageAware gui) {
        return previousPageBase(player)
                .setName(Component.translatable("spectatorMenu.previous_page").withStyle(ChatFormatting.WHITE))
                .noDefaults()
                .hideDefaultTooltip()
                .setCallback((x, y, z) -> {
                    playClickSound(player);
                    gui.previousPage();
                }).build();
    }

    public static boolean hasTexture(ServerPlayer player) {
        return PolymerResourcePackUtils.hasMainPack(player);
    }

    public static GuiElementInterface fillerStack(ServerPlayer player) {
        return hasTexture(player) ? EMPTY_STACK : FILLER;
    }

    public static Component formatTexturedText(ServerPlayer player, @Nullable Component texture, @Nullable Component input) {
        if (PolymerResourcePackUtils.hasMainPack(player)) {
            var text = Component.empty();
            var textTexture = Component.empty().setStyle(TEXTURE_STYLE);

            if (texture != null) {
                textTexture.append("a").append(texture).append("b");
            }

            if (!textTexture.getSiblings().isEmpty()) {
                text.append(textTexture);
            }

            if (input != null) {
                text.append(input);
            }
            return text;
        } else {
            return input != null ? input : Component.empty();
        }
    }
}
