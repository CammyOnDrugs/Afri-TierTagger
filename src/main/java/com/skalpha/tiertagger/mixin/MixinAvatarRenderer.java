package com.skalpha.tiertagger.mixin;

import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.config.TierTaggerConfig;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class MixinAvatarRenderer {
   @Inject(method = "updateRenderState", at = @At("TAIL"))
   private void onExtractRenderState(PlayerLikeEntity entity, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
      if (((TierTaggerConfig)TierTagger.getManager().getConfig()).isEnabled()) {
         if (entity instanceof PlayerEntity player) {
            if (state instanceof EntityRenderStateAccessor acc) {
               Text nameTag = acc.getNameTag();
               if (nameTag != null) {
                  acc.setNameTag(TierTagger.appendTier(player.getUuid(), nameTag));
               }
            }
         }
      }
   }
}
