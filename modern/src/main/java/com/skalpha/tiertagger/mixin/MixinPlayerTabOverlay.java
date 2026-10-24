package com.skalpha.tiertagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.config.TierTaggerConfig;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlay {
   @ModifyReturnValue(method = "getNameForDisplay", at = @At("RETURN"))
   @Nullable
   public Component prependTier(Component original, PlayerInfo entry) {
      TierTaggerConfig config = (TierTaggerConfig)TierTagger.getManager().getConfig();
      return config.isEnabled() && config.isPlayerList() ? TierTagger.appendTier(entry.getProfile().id(), original) : original;
   }
}
