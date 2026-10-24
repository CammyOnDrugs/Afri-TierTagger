package com.skalpha.tiertagger.config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public abstract class CloseableScreen extends Screen {
    protected final Screen parent;
    protected CloseableScreen(String title, Screen parent) { this(Component.literal(title), parent); }
    protected CloseableScreen(Component title, Screen parent) { super(title); this.parent = parent; }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
}
