package com.skalpha.tiertagger.config;

import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.uku3lig.ukulib.config.option.WidgetCreator;
import net.uku3lig.ukulib.config.option.widget.TextInputWidget;

public record ColorInputOption(String key, String initial, Consumer<String> setter) implements WidgetCreator {
    @Override public TextInputWidget createWidget(int x, int y, int width, int height) {
        return new TextInputWidget(x, y, width - 24, height, initial, setter, Text.translatable(key).getString(), TierTaggerConfig::isValidColor, 8) {
            @Override public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                super.renderWidget(context, mouseX, mouseY, delta);
                int left = getX() + getWidth() + 5;
                int top = getY() + (getHeight() - 14) / 2;
                context.fill(left, top, left + 14, top + 14, 0xFFF0F0F0);
                int color = TierTaggerConfig.isValidColor(getText()) ? 0xFF000000 | TierTaggerConfig.parseColor(getText()) : 0xFF444444;
                context.fill(left + 1, top + 1, left + 13, top + 13, color);
            }
        };
    }
}
