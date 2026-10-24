package com.skalpha.tiertagger.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerInfoTest {
    @Test void currentPublicApiContractUsesCurrentTierAndCanonicalModes() {
        var players = PlayerInfo.parsePlayersJs("""
          {"players":[{"name":"Example","uuid":"00000000-0000-0000-0000-000000000001",
          "totalPoints":6,"tiers":[{"mode":"spear","level":"LT3","retired":false,"peakTier":"HT1"},
          {"mode":"nethpot","level":"HT4","retired":true}]}],"source":"database","stale":false}
          """);
        assertEquals(1,players.size());
        assertEquals("LT3",players.getFirst().rankings().get("spear").tierName());
        assertTrue(players.getFirst().rankings().get("nethpot").retired());
        assertFalse(players.getFirst().rankings().containsKey("peakTier"));
    }
    @Test void legacyModeSettingsResolveToLiveModeIds() {
        assertEquals("diapot",GameMode.byId("pot").id());
        assertEquals("diapot",GameMode.byId("dia_pot").id());
        assertEquals("nethpot",GameMode.byId("nethop").id());
        assertEquals("spear",GameMode.byId("diamond_spear").id());
        assertEquals("cart",GameMode.byId("minecart").id());
        assertEquals("diasmp",GameMode.byId("dia_smp").id());
    }
    @Test void overallPositionsUseTotalPointsAndStayTheSameAcrossGamemodes() {
        var players = PlayerInfo.parsePlayersJs("""
          {"players":[
            {"name":"Lower","uuid":"1","totalPoints":6,"tiers":[{"mode":"sword","level":"LT3","points":6}]},
            {"name":"Retired","uuid":"2","totalPoints":0,"tiers":[{"mode":"sword","level":"HT1","points":0,"retired":true}]},
            {"name":"HigherSword","uuid":"3","totalPoints":10,"tiers":[{"mode":"sword","level":"HT3","points":10}]},
            {"name":"OverallFirst","uuid":"4","totalPoints":20,"tiers":[{"mode":"sword","level":"LT3","points":6},{"mode":"axe","level":"HT3","points":10},{"mode":"mace","level":"HT4","points":4}]},
            {"name":"TwoModes","uuid":"5","totalPoints":11,"tiers":[{"mode":"sword","level":"HT3","points":10},{"mode":"axe","level":"LT5","points":1}]},
            {"name":"NoPlacements","uuid":"6","totalPoints":0,"tiers":[]}
          ]}
          """);
        assertEquals(4,players.get(0).rankings().get("sword").position());
        assertEquals(5,players.get(1).rankings().get("sword").position());
        assertEquals(3,players.get(2).rankings().get("sword").position());
        assertEquals(1,players.get(3).rankings().get("sword").position());
        assertEquals(1,players.get(3).rankings().get("axe").position());
        assertEquals(1,players.get(3).rankings().get("mace").position());
        assertEquals(2,players.get(4).rankings().get("sword").position());
        assertEquals(2,players.get(4).rankings().get("axe").position());
        assertEquals(1,players.get(3).overall());
        assertEquals(0,players.get(5).overall());
    }
    @Test void overallTiesKeepTheWebsitesSourceOrder() {
        var players = PlayerInfo.parsePlayersJs("""
          {"players":[
            {"name":"Zulu","uuid":"1","totalPoints":6,"tiers":[{"mode":"sword","level":"LT3"}]},
            {"name":"Alpha","uuid":"2","totalPoints":6,"tiers":[{"mode":"axe","level":"LT3"}]}
          ]}
          """);
        assertEquals(1,players.get(0).overall());
        assertEquals(2,players.get(1).overall());
    }
}
