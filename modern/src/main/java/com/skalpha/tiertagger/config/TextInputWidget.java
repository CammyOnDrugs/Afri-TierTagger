package com.skalpha.tiertagger.config;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
public class TextInputWidget extends EditBox {
    private final Predicate<String> validator;
    public TextInputWidget(int x, int y, int width, int height, String initial, Consumer<String> setter, String label, Predicate<String> validator, int max) {
        super(Minecraft.getInstance().font, x, y, width, height, Component.literal(label));
        this.validator = validator;
        setMaxLength(max); setValue(initial); setResponder(value -> { if (validator.test(value)) setter.accept(value); });
    }
    public String getText() { return getValue(); }
    public void setText(String value) { setValue(value); }
    public boolean isValid() { return validator.test(getValue()); }
}
