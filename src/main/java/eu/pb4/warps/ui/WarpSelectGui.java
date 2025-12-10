package eu.pb4.warps.ui;

import eu.pb4.predicate.api.PredicateContext;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.warps.WarpManager;
import eu.pb4.warps.data.WarpData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class WarpSelectGui extends PagedGui {
    private final List<WarpData> warps = new ArrayList<>();

    protected WarpSelectGui(ServerPlayer player) {
        super(player, null);
        this.setTitle(GuiUtils.formatTexturedText(player, Component.literal("e"), Component.translatable("gui.pbwarps.warp_selector")));

        var ctx = PredicateContext.of(player);
        for (var warp : WarpManager.get().warps()) {
            if (warp.predicate().isEmpty() || warp.predicate().get().test(ctx).success()) {
                this.warps.add(warp);
            }
        }

        this.updateDisplay();
    }

    public static void open(ServerPlayer player) {
        new WarpSelectGui(player).open();
    }

    @Override
    public int getPageAmount() {
        return Math.max((this.warps.size() - 1)/ PAGE_SIZE + 1, 1);
    }

    @Override
    protected GuiElementInterface getElement(int id) {
        if (this.warps.size() > id) {
            var warp = this.warps.get(id);

            var icon = GuiElementBuilder.from(warp.icon());
            icon.setName(warp.name().text());

            icon.setCallback((x, y, z) -> {
                playClickSound(player);
                this.close();
                warp.handleTeleport(player);
            });

            return icon.build();
        }

        return GuiUtils.EMPTY;
    }


}
