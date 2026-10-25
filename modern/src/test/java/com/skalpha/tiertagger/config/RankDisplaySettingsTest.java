package com.skalpha.tiertagger.config;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RankDisplaySettingsTest {
 @Test void rankNumbersAndColorsPersistButIconsStayRequired(){
  TierTaggerConfig config=new TierTaggerConfig();assertTrue(config.isRankNumbers());assertTrue(config.isRankTrophies());assertTrue(config.isPodiumColors());
  config.setShowIcons(false);config.setRankNumbers(false);config.setRankTrophies(false);config.setPodiumColors(false);
  Gson gson=new Gson();TierTaggerConfig saved=gson.fromJson(gson.toJson(config),TierTaggerConfig.class);
  assertFalse(saved.isRankNumbers());assertTrue(saved.isRankTrophies());assertFalse(saved.isPodiumColors());assertTrue(saved.isShowIcons());
  TierTaggerConfig old=gson.fromJson("{\"showIcons\":false,\"rankTrophies\":false}",TierTaggerConfig.class);assertTrue(old.isShowIcons());assertTrue(old.isRankTrophies());
 }
}
