package com.skalpha.tiertagger.model;

import com.google.gson.JsonParser;
import com.skalpha.tiertagger.tierlist.Leaderboard;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortraitAndLeaderboardTest {
    private String portrait(String json){return PortraitSource.forPlayer(JsonParser.parseString(json).getAsJsonObject());}
    @Test void paidPortraitUsesVerifiedWebsiteIdentityAndCanonicalBust(){
        assertEquals("https://visage.surgeplay.com/bust/256/51cf1c94674d4b42ba66bc88ae843a62.png",portrait("""
            {"accountType":"paid","uuid":"old_name","minecraftProfileUuid":"51cf1c94-674d-4b42-ba66-bc88ae843a62","skinUrl":"http://untrusted.invalid/portrait"}
            """));
    }
    @Test void crackedAndUnknownProfilesUseTheSuppliedWebsiteFallback(){
        assertEquals(PortraitSource.FALLBACK,portrait("{\"accountType\":\"cracked\",\"minecraftProfileUuid\":\"51cf1c94674d4b42ba66bc88ae843a62\"}"));
        assertEquals(PortraitSource.FALLBACK,portrait("{\"name\":\"Unknown\"}"));
        assertEquals(PortraitSource.FALLBACK,portrait("{\"accountType\":\"paid\",\"linkedAccount\":{\"accountType\":\"cracked\"}}"));
    }
    @Test void imageSourcesCannotRedirectDownloadsToArbitraryHosts(){
        assertTrue(PortraitSource.allowed(PortraitSource.FALLBACK));
        assertFalse(PortraitSource.allowed("https://visage.surgeplay.com@other.invalid/image.png"));
        assertFalse(PortraitSource.allowed("https://visage.surgeplay.com/bust/256/../../image.png"));
        assertFalse(PortraitSource.allowed("file:///secret.png"));
    }
    private List<PlayerInfo> players(){return PlayerInfo.parsePlayersJs("""
        {"players":[
          {"name":"Alpha","totalPoints":100,"accountType":"paid","minecraftProfileUuid":"51cf1c94674d4b42ba66bc88ae843a62","tiers":[{"mode":"sword","level":"LT2"}]},
          {"name":"Beta","totalPoints":80,"tiers":[{"mode":"sword","level":"HT1"}]},
          {"name":"Gamma","totalPoints":40,"tiers":[{"mode":"axe","level":"HT1"}]},
          {"name":"Retired","totalPoints":0,"tiers":[{"mode":"sword","level":"HT1","retired":true}]}]}
        """);}
    @Test void overallAndModeViewsKeepOverallPositionsAndUseTierOrder(){
        var source=players();assertEquals(List.of("Alpha","Beta","Gamma","Retired"),Leaderboard.rows(source,"overall","").stream().map(PlayerInfo::name).toList());
        var sword=Leaderboard.rows(source,"sword","");assertEquals(List.of("Beta","Alpha","Retired"),sword.stream().map(PlayerInfo::name).toList());
        assertEquals(2,sword.getFirst().overall());assertEquals(1,sword.get(1).overall());
    }
    @Test void playerSearchIsCaseInsensitiveAndRestrictedToSelectedMode(){
        assertEquals(List.of("Alpha"),Leaderboard.rows(players(),"overall","ALP").stream().map(PlayerInfo::name).toList());
        assertTrue(Leaderboard.rows(players(),"axe","Alpha").isEmpty());
        assertThrows(IllegalArgumentException.class,()->Leaderboard.rows(players(),"made-up",""));
    }
    @Test void portraitSurvivesOverallPositionAssignment(){assertEquals("https://visage.surgeplay.com/bust/256/51cf1c94674d4b42ba66bc88ae843a62.png",players().getFirst().portrait());}
}
