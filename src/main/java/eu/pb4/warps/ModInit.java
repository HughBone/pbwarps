package eu.pb4.warps;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.warps.ui.GuiUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public class ModInit implements ModInitializer {
    /** AllOutJay — the built-in warp admin and default owner of legacy/ownerless warps. */
    public static final UUID ALLOUTJAY_UUID = UUID.fromString("9e099716-9ebd-4390-91e0-dc162e35387b");

    public static Identifier id(String s) {
        return Identifier.fromNamespaceAndPath("pbwarps", s);
    }

    @Override
    public void onInitialize() {
        PolymerResourcePackUtils.addModAssets("pbwarps");
        GuiUtils.register();
        CommandRegistrationCallback.EVENT.register(WarpCommands::init);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> WarpAdmins.load());
        ServerLifecycleEvents.SERVER_STARTING.register(WarpManager::setup);
        ServerLifecycleEvents.SERVER_STOPPED.register((x) -> WarpManager.destroy());
        ServerLifecycleEvents.BEFORE_SAVE.register((server, a, b) -> WarpManager.get().save());
    }
}
