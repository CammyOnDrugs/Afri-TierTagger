package com.skalpha.tiertagger.config;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
public final class ClientUi {
    public static void registerKeybinding(KeyMapping key, Consumer<Minecraft> action) {
        KeyBindingHelper.registerKeyBinding(key);
        ClientTickEvents.END_CLIENT_TICK.register(client -> { while (key.consumeClick()) action.accept(client); });
    }
    public static void sendToast(Component message, Object unused) {
        Minecraft.getInstance().execute(() -> { if (Minecraft.getInstance().player != null) Minecraft.getInstance().player.displayClientMessage(message, false); });
    }
}
