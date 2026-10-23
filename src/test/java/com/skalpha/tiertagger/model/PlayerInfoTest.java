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
}
