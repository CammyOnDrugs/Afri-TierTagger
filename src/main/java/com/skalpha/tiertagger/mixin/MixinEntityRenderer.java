package com.skalpha.tiertagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.config.TierTaggerConfig;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
   @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
   public Text prependTier(Text original, Entity entity) {
      return ((TierTaggerConfig)TierTagger.getManager().getConfig()).isEnabled()
            && entity instanceof PlayerEntity player
            && original != null
            && !original.getString().contains(" | ")
         ? TierTagger.appendTier(player.getUuid(), original)
         : original;
   }

   @Inject(method = "updateRenderState", at = @At("TAIL"))
   public void prependTierToNameTag(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
      if (((TierTaggerConfig)TierTagger.getManager().getConfig()).isEnabled()
         && entity instanceof PlayerEntity player
         && state.displayName != null
         && !state.displayName.getString().contains(" | ")) {
         state.displayName = TierTagger.appendTier(player.getUuid(), state.displayName);
      }
   }
}
