package com.skalpha.tiertagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.config.TierTaggerConfig;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerListHud.class)
public class MixinPlayerTabOverlay {
   @ModifyReturnValue(method = "getPlayerName", at = @At("RETURN"))
   @Nullable
   public Text prependTier(Text original, PlayerListEntry entry) {
      TierTaggerConfig config = (TierTaggerConfig)TierTagger.getManager().getConfig();
      return config.isEnabled() && config.isPlayerList() ? TierTagger.appendTier(entry.getProfile().id(), original) : original;
   }
}
