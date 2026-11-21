package dev.pvpmacro.mc;

import com.mojang.brigadier.CommandDispatcher;
import dev.pvpmacro.config.ConfigManager;
import dev.pvpmacro.config.PvpMacroConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

/** /pvpmacro reload | status | toggle  (client-side only, nothing is sent to the server). */
public final class Commands {

    private Commands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, MacroEngine engine) {
        dispatcher.register(ClientCommandManager.literal("pvpmacro")
                .then(ClientCommandManager.literal("reload").executes(ctx -> {
                    String warning = ConfigManager.load();
                    ctx.getSource().sendFeedback(Text.literal(
                            warning == null ? "[PvPMacro] config reloaded" : "[PvPMacro] " + warning));
                    return 1;
                }))
                .then(ClientCommandManager.literal("toggle").executes(ctx -> {
                    PvpMacroConfig cfg = ConfigManager.get();
                    cfg.masterEnabled = !cfg.masterEnabled;
                    ConfigManager.save();
                    ctx.getSource().sendFeedback(Text.literal(
                            "[PvPMacro] macros " + (cfg.masterEnabled ? "ON" : "OFF")));
                    return 1;
                }))
                .then(ClientCommandManager.literal("status").executes(ctx -> {
                    PvpMacroConfig cfg = ConfigManager.get();
                    ctx.getSource().sendFeedback(Text.literal(
                            "[PvPMacro] master=" + cfg.masterEnabled
                                    + " attributeSwap=" + cfg.attributeSwap.enabled
                                    + " running=" + engine.isRunning()));
                    return 1;
                })));
    }
}
