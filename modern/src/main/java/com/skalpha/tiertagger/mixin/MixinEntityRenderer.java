package com.skalpha.tiertagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.config.TierTaggerConfig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
   @ModifyReturnValue(method = "getNameTag", at = @At("RETURN"))
   public Component prependTier(Component original, Entity entity) {
      return ((TierTaggerConfig)TierTagger.getManager().getConfig()).isEnabled()
            && TierTagger.getManager().getConfig().isNameTags()
            && entity instanceof Player player
            && original != null
            && !original.getString().contains(" | ")
         ? TierTagger.appendTier(player.getUUID(), original)
         : original;
   }

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   public void prependTierToNameTag(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
      var client=net.minecraft.client.Minecraft.getInstance();
      if(TierTagger.getManager().getConfig().isEnabled()&&TierTagger.getManager().getConfig().isNameTags()&&TierTagger.getManager().getConfig().isShowOwnTier()&&entity==client.player&&!client.options.getCameraType().isFirstPerson()&&!state.isInvisible){
         state.nameTag=entity.getName();if(!state.nameTag.getString().contains(" | "))state.nameTag=TierTagger.appendTier(entity.getUUID(),state.nameTag);state.nameTagAttachment=new net.minecraft.world.phys.Vec3(0,state.boundingBoxHeight+0.5,0);
      }

      if (((TierTaggerConfig)TierTagger.getManager().getConfig()).isEnabled()
         && TierTagger.getManager().getConfig().isNameTags()
         && entity instanceof Player player
         && state.nameTag != null
         && !state.nameTag.getString().contains(" | ")) {
         state.nameTag = TierTagger.appendTier(player.getUUID(), state.nameTag);
      }
   }
}
