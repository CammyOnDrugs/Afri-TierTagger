package com.skalpha.tiertagger.config;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.uku3lig.ukulib.config.option.WidgetCreator;
public record ColorInputOption(String key,String initial,Consumer<String> setter) implements WidgetCreator {
    public ButtonWidget createWidget(int x,int y,int width,int height){
        return ButtonWidget.builder(Text.translatable(key).append(": "+initial),b->{var client=MinecraftClient.getInstance();client.setScreen(new LegacyColorScreen(client.currentScreen,Text.translatable(key).getString(),initial,setter));}).dimensions(x,y,width,height).build();
    }
}
