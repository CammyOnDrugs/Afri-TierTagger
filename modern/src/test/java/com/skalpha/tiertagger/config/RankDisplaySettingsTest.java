package com.skalpha.tiertagger.config;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RankDisplaySettingsTest {
 @Test void settingsDefaultOnAndPersistIndependently(){
  TierTaggerConfig config=new TierTaggerConfig();assertTrue(config.isRankNumbers());assertTrue(config.isRankTrophies());assertTrue(config.isPodiumColors());
  config.setRankNumbers(false);config.setRankTrophies(false);config.setPodiumColors(false);
  Gson gson=new Gson();TierTaggerConfig saved=gson.fromJson(gson.toJson(config),TierTaggerConfig.class);
  assertFalse(saved.isRankNumbers());assertFalse(saved.isRankTrophies());assertFalse(saved.isPodiumColors());assertTrue(saved.isShowIcons());
 }
}
