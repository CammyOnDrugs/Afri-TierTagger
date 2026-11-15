package com.skalpha.tiertagger.model;
import com.skalpha.tiertagger.config.TierTaggerConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AppearanceTest {
    @Test void modeVisibilityAndColoursSurviveConfigurationRoundTrip(){
        var config=new TierTaggerConfig();
        assertTrue(config.allowsMode("spear"));assertTrue(config.allowsMode("rocketmace"));
        config.setShowMainTiers(false);config.setShowOwnTier(true);config.setShowPeakTiers(false);config.setLiquidGlass(false);config.setMenuColor(0x123456);
        var gson=new com.google.gson.Gson();var restored=gson.fromJson(gson.toJson(config),TierTaggerConfig.class);
        assertFalse(restored.allowsMode("sword"));assertTrue(restored.allowsMode("holefight"));
        assertTrue(restored.isShowOwnTier());assertFalse(restored.isShowPeakTiers());assertFalse(restored.isLiquidGlass());assertEquals(0x123456,restored.getMenuColor());
    }
    @Test void allFourIconSetsUseDistinctHighPrivateUseCharacters(){
        var modes=com.skalpha.tiertagger.tierlist.Leaderboard.MODES.stream().filter(id->!id.equals("overall")).toList();
        var glyphs=new java.util.HashSet<String>();int before=GameMode.iconStyle;
        try{for(int style=0;style<4;style++){GameMode.iconStyle=style;for(String mode:modes){String glyph=GameMode.byId(mode).icon().orElseThrow();assertTrue(glyph.codePointAt(0)>=0x10FFB6&&glyph.codePointAt(0)<=0x10FFFD);assertTrue(glyphs.add(glyph));}}}finally{GameMode.iconStyle=before;}
        assertEquals(72,glyphs.size());
    }
    @Test void earnedPeakIsPreservedWithoutChangingPoints(){
        var players=PlayerInfo.parsePlayersJs("{\"players\":[{\"name\":\"Player\",\"totalPoints\":3,\"tiers\":[{\"mode\":\"sword\",\"level\":\"LT4\",\"points\":3,\"peakTier\":\"HT3\"}]}]}");
        var ranking=players.getFirst().rankings().get("sword");assertEquals("HT3",ranking.peakTier());assertEquals(3,ranking.points());
    }
}
