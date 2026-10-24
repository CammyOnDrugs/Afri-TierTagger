package com.skalpha.tiertagger.mixin;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderState.class)
public interface EntityRenderStateAccessor {
   @Accessor("nameTag")
   Component getNameTag();

   @Accessor("nameTag")
   void setNameTag(Component var1);
}
