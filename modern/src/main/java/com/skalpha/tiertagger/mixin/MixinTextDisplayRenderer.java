package com.skalpha.tiertagger.mixin;

import com.skalpha.tiertagger.TierCache;
import com.skalpha.tiertagger.TierTagger;
import java.util.Optional;
import net.minecraft.client.renderer.entity.DisplayRenderer.TextDisplayRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TextDisplayRenderer.class)
public class MixinTextDisplayRenderer {
   @ModifyArg(
      method = "splitLines",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"),
      index = 0
   )
   public FormattedText editText(FormattedText original) {
      Component text = (Component)original;
      String stringText = text.getString();
      if (stringText.isBlank()) {
         return original;
      } else {
         Optional<String> cached = TierCache.findCachedNameInText(stringText);
         if (cached.isPresent()) {
            String name = cached.get();
            int index = stringText.indexOf(name);
            if (!this.isSurrounded(stringText, index, name.length())) {
               return TierTagger.appendTierByName(name, text);
            }
         }

         return original;
      }
   }

   @Unique
   private boolean isSurrounded(String stringText, int index, int length) {
      return index == -1
         || index > 0 && Character.isLetterOrDigit(stringText.charAt(index - 1))
         || index + length < stringText.length() && Character.isLetterOrDigit(stringText.charAt(index + length));
   }
}
