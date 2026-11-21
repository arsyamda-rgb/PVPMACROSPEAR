package dev.pvpmacro;

import dev.pvpmacro.config.ConfigManager;
import dev.pvpmacro.mc.Commands;
import dev.pvpmacro.mc.Keys;
import dev.pvpmacro.mc.MacroEngine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PvpMacroClient implements ClientModInitializer {

    public static final String MOD_ID = "pvpmacro";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        String warning = ConfigManager.load();
        if (warning != null) {
            LOG.warn(warning);
        }
        Keys.register();

        MacroEngine engine = new MacroEngine();
        ClientTickEvents.START_CLIENT_TICK.register(engine::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(engine::onEndTick);
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> Commands.register(dispatcher, engine));

        LOG.info("PvP Macro loaded");
    }
}
