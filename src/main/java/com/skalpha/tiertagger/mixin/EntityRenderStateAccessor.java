package com.skalpha.tiertagger.mixin;

import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderState.class)
public interface EntityRenderStateAccessor {
   @Accessor("displayName")
   Text getNameTag();

   @Accessor("displayName")
   void setNameTag(Text var1);
}
