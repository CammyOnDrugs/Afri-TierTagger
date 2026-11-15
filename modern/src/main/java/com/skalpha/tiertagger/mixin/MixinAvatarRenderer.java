package com.skalpha.tiertagger.mixin;

import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.config.TierTaggerConfig;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class MixinAvatarRenderer {
   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void onExtractRenderState(Avatar entity, AvatarRenderState state, float tickProgress, CallbackInfo ci) {
      if (((TierTaggerConfig)TierTagger.getManager().getConfig()).isEnabled()) {
         if (entity instanceof Player player) {
            var client=net.minecraft.client.Minecraft.getInstance();
            if(player==client.player&&TierTagger.getManager().getConfig().isShowOwnTier()&&!client.options.getCameraType().isFirstPerson()&&!state.isInvisible){
                state.nameTag=player.getName();state.nameTagAttachment=new net.minecraft.world.phys.Vec3(0,state.boundingBoxHeight+0.5,0);
            }
            if (state instanceof EntityRenderStateAccessor acc) {
               Component nameTag = acc.getNameTag();
               if (nameTag != null) {
                  acc.setNameTag(TierTagger.appendTier(player.getUUID(), nameTag));
               }
            }
         }
      }
   }
}
