package com.skalpha.tiertagger.mixin;

import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.config.TierTaggerConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class MixinPlayer {
   private static volatile boolean configEnabled = true;
   private static volatile long lastConfigCheck = 0L;

   @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
   public void prependTierToDisplay(CallbackInfoReturnable<Text> cir) {
      Text original = (Text)cir.getReturnValue();
      if (original != null && this.isEnabled()) {
         if (!original.getString().contains(" | ")) {
            PlayerEntity self = (PlayerEntity)(Object)this;
            cir.setReturnValue(TierTagger.appendTier(self.getUuid(), original));
         }
      }
   }

   @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
   public void prependTierToName(CallbackInfoReturnable<Text> cir) {
      Text original = (Text)cir.getReturnValue();
      if (original != null && this.isEnabled()) {
         if (!original.getString().contains(" | ")) {
            PlayerEntity self = (PlayerEntity)(Object)this;
            cir.setReturnValue(TierTagger.appendTier(self.getUuid(), original));
         }
      }
   }

   private boolean isEnabled() {
      long now = System.currentTimeMillis();
      if (now - lastConfigCheck > 5000L) {
         configEnabled = ((TierTaggerConfig)TierTagger.getManager().getConfig()).isEnabled();
         lastConfigCheck = now;
      }

      return configEnabled;
   }
}
