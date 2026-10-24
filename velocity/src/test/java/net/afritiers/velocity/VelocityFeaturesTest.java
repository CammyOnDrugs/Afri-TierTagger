package net.afritiers.velocity;

import com.google.gson.*;
import com.velocitypowered.api.proxy.*;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.NameTagVisibility;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VelocityFeaturesTest {
    @TempDir Path directory;
    private static final UUID PLAYER=UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
    private String sample(){return """
        {"players":[
        {"uuid":"00112233445566778899aabbccddeeff","name":"Zulu","region":"AF","totalPoints":50,"tiers":[{"mode":"sword","level":"LT2","retired":false},{"mode":"axe","level":"HT3","retired":false}]},
        {"uuid":"11112233445566778899aabbccddeeff","name":"Alpha","region":"AF","totalPoints":50,"tiers":[{"mode":"mace","level":"HT2","retired":false}]},
        {"uuid":"22222233445566778899aabbccddeeff","name":"Third","region":"AF","totalPoints":10,"tiers":[{"mode":"sword","level":"LT3","retired":false}]},
        {"uuid":"33332233445566778899aabbccddeeff","name":"Fourth","region":"AF","totalPoints":1,"tiers":[{"mode":"smp","level":"LT5","retired":false}]},
        {"uuid":"44442233445566778899aabbccddeeff","name":"Unranked","region":"AF","totalPoints":0,"tiers":[]}]}
        """;}
    @Test void overallPositionsAreStableAndIndependentOfSelectedMode(){
        Rankings rankings=new Rankings();rankings.update(sample());var prefs=new Preferences();
        var profile=rankings.find(PLAYER,"ignored").orElseThrow();assertEquals(1,profile.position());assertEquals(2,rankings.find(null,"Alpha").orElseThrow().position());
        prefs.gameMode="sword";String sword=rankings.tag(profile,prefs,false,null).toString();prefs.gameMode="axe";String axe=rankings.tag(profile,prefs,false,null).toString();
        assertTrue(sword.contains("#1"));assertTrue(axe.contains("#1"));assertEquals(0,rankings.find(null,"Unranked").orElseThrow().position());
        assertArrayEquals(new int[]{0xFFD700,0xC0C0C0,0xCD7F32,0xAAAAAA},new int[]{Rankings.positionColor(1),Rankings.positionColor(2),Rankings.positionColor(3),Rankings.positionColor(20)});
    }
    @Test void disabledAndMissingTierRespectPreferences(){
        Rankings rankings=new Rankings();rankings.update(sample());var prefs=new Preferences();var profile=rankings.all().getFirst();
        prefs.highest="never";prefs.gameMode="mace";assertEquals(net.kyori.adventure.text.Component.empty(),rankings.tag(profile,prefs,false,null));
        prefs.gameMode="sword";prefs.enabled=false;assertEquals(net.kyori.adventure.text.Component.empty(),rankings.tag(profile,prefs,false,null));
    }
    @Test void rankingNumbersTrophiesAndPodiumColorsCanBeControlledIndependently(){
        Rankings rankings=new Rankings();rankings.update(sample());var prefs=new Preferences();var profile=rankings.all().getFirst();
        SettingsCommand.change(prefs,new String[]{"rank-numbers","off"});
        var trophyOnly=rankings.tag(profile,prefs,true,null);assertFalse(trophyOnly.toString().contains("#1"));assertTrue(trophyOnly.toString().contains("\uE901"));
        SettingsCommand.change(prefs,new String[]{"rank-trophies","off"});assertFalse(rankings.tag(profile,prefs,true,null).toString().contains("\uE901"));
        SettingsCommand.change(prefs,new String[]{"rank-numbers","on"});SettingsCommand.change(prefs,new String[]{"podium-colors","off"});
        var gray=rankings.tag(profile,prefs,true,null);assertEquals(0xAAAAAA,gray.children().getFirst().color().value());
    }
    @Test void settingsPersistPerUuidAndRejectInvalidInput()throws Exception{
        var store=new Preferences.Store(directory);SettingsCommand.change(store.get(PLAYER),new String[]{"color","HT1","#abc"});SettingsCommand.change(store.get(PLAYER),new String[]{"ticket","off"});store.save(PLAYER);
        var reloaded=new Preferences.Store(directory);assertEquals(0xAABBCC,reloaded.get(PLAYER).colors.get("HT1"));assertFalse(reloaded.get(PLAYER).ticketAlerts);assertTrue(reloaded.get(UUID.randomUUID()).ticketAlerts);
        assertThrows(IllegalArgumentException.class,()->SettingsCommand.change(store.get(PLAYER),new String[]{"mode","staff_command"}));
        assertThrows(IllegalArgumentException.class,()->SettingsCommand.change(store.get(PLAYER),new String[]{"color","HT1","#evil"}));
    }
    @Test void queueForwardsOnlyModeThroughActualServerSession()throws Exception{
        var plugin=new AfriTiersVelocity(mock(ProxyServer.class),mock(Logger.class),directory);Player player=mock(Player.class);when(player.getCurrentServer()).thenReturn(Optional.of(mock(ServerConnection.class)));
        plugin.join(player,new String[]{"pot"});verify(player).spoofChatInput("/afritierslink:join-queue diapot");
        plugin.join(player,new String[]{"sword","OtherPlayer"});verifyNoMoreInteractionsIgnoringStubs(player);
    }
    private void verifyNoMoreInteractionsIgnoringStubs(Player player){verify(player,never()).spoofChatInput(contains("OtherPlayer"));}
    @Test void clientCannotInjectTicketNotifications(){
        var plugin=new AfriTiersVelocity(mock(ProxyServer.class),mock(Logger.class),directory);Player player=mock(Player.class);
        var event=new PluginMessageEvent(player,player,AfriTiersVelocity.QUEUE,AfriTiersVelocity.encode("{\"type\":\"tickets\",\"tickets\":[]}"));
        plugin.message(event);assertFalse(event.getResult().isAllowed());
    }
    @Test void backendTicketsArePrivateAndDeduplicated()throws Exception{
        var plugin=new AfriTiersVelocity(mock(ProxyServer.class),mock(Logger.class),directory);plugin.preferences=new Preferences.Store(directory);
        Player player=mock(Player.class);ServerConnection connection=mock(ServerConnection.class);when(player.getUniqueId()).thenReturn(PLAYER);when(player.getUsername()).thenReturn("Zulu");when(player.getCurrentServer()).thenReturn(Optional.of(connection));
        var session=new AfriTiersVelocity.Session();plugin.sessions.put(PLAYER,session);
        String payload="{\"type\":\"tickets\",\"tickets\":[{\"eventId\":\""+"a".repeat(64)+"\",\"username\":\"Zulu\",\"mode\":\"sword\"}]}";
        plugin.message(new PluginMessageEvent(connection,player,AfriTiersVelocity.QUEUE,AfriTiersVelocity.encode(payload)));
        plugin.message(new PluginMessageEvent(connection,player,AfriTiersVelocity.QUEUE,AfriTiersVelocity.encode(payload)));
        assertEquals(1,session.notices.size());
        plugin.message(new PluginMessageEvent(connection,player,AfriTiersVelocity.QUEUE,AfriTiersVelocity.encode(payload.replace("Zulu","Alpha").replace("a".repeat(64),"b".repeat(64)))));
        assertEquals(1,session.notices.size());
    }
    @Test void pluginPayloadLengthsAreValidated(){
        String payload="{\"type\":\"tickets\",\"tickets\":[]}";assertEquals(payload,AfriTiersVelocity.decode(AfriTiersVelocity.encode(payload)));
        assertThrows(IllegalArgumentException.class,()->AfriTiersVelocity.decode(new byte[]{127,1}));
        assertThrows(IllegalArgumentException.class,()->AfriTiersVelocity.decode(new byte[]{(byte)255,(byte)255,(byte)255,(byte)255,(byte)255}));
    }
    @Test void backendTeamVisibilityIsPreservedForEachViewer(){
        assertEquals(NameTagVisibility.NEVER,TagRenderer.visibility(NameTagVisibility.HIDE_FOR_OTHER_TEAMS,"a","b"));
        assertEquals(NameTagVisibility.ALWAYS,TagRenderer.visibility(NameTagVisibility.HIDE_FOR_OTHER_TEAMS,"a","a"));
        assertEquals(NameTagVisibility.NEVER,TagRenderer.visibility(NameTagVisibility.HIDE_FOR_OWN_TEAM,"a","a"));
        assertEquals(NameTagVisibility.NEVER,TagRenderer.visibility(NameTagVisibility.NEVER,null,null));
    }
}
