package com.skalpha.tiertagger.config;

import java.util.function.UnaryOperator;
import net.minecraft.client.gui.screen.Screen;
import net.uku3lig.ukulib.api.UkulibAPI;

public class UkulibIntegration implements UkulibAPI {
   public UnaryOperator<Screen> supplyConfigScreen() {
      return TTConfigScreen::new;
   }
}
