package net.afritiers.purpur;
import com.skalpha.tiertagger.queue.QueueFeed;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalMatchers.aryEq;

class PurpurFeaturesTest {
    @Test void ticketSignatureMatchesBackendFixture()throws Exception{
        assertEquals("f57dcd063b598d070b6d3ac9e867b999c6a02fe81363ffcd5cd234e1f83335d8",TicketService.sign("test-only-key-with-at-least-32-bytes","123","abc","{\"action\":\"status\"}"));
    }
    @TempDir Path directory;
    @Test void tabCompletionUsesOnlyOpenQueues(){
        AfriTiersPurpur plugin=mock(AfriTiersPurpur.class,CALLS_REAL_METHODS);var now=Instant.now();var link=URI.create("https://discord.com/channels/123456789012345678/123456789012345679");
        plugin.setQueues(List.of(new QueueFeed.Entry("sword",true,"a".repeat(32),now,link),new QueueFeed.Entry("smp",false,"b".repeat(32),now,link),new QueueFeed.Entry("diavanilla",true,"c".repeat(32),now,link)));
        Command command=mock(Command.class);when(command.getName()).thenReturn("join-queue");
        assertEquals(List.of("diavanilla","sword"),plugin.onTabComplete(mock(Player.class),command,"join-queue",new String[]{""}));
        assertEquals(List.of("sword"),plugin.onTabComplete(mock(Player.class),command,"afritierslink:join-queue",new String[]{"S"}));
        assertTrue(plugin.onTabComplete(mock(Player.class),command,"join-queue",new String[]{"sm"}).isEmpty());
    }
    @Test void joinUsesActualPlayerSessionAndNormalBackendChecks(){
        AfriTiersPurpur plugin=mock(AfriTiersPurpur.class,CALLS_REAL_METHODS);Player player=mock(Player.class);PluginCommand backend=mock(PluginCommand.class);
        try(var bukkit=mockStatic(Bukkit.class)){
            bukkit.when(()->Bukkit.getPluginCommand("afritierslink:join-queue")).thenReturn(backend);
            plugin.join(player,new String[]{"pot"});verify(backend).execute(same(player),eq("afritierslink:join-queue"),aryEq(new String[]{"diapot"}));
            plugin.join(player,new String[]{"sword","OtherPlayer"});verify(backend,times(1)).execute(any(),anyString(),any());
        }
    }
    @Test void noBackendNeverClaimsAQueueJoinSucceeded(){
        AfriTiersPurpur plugin=mock(AfriTiersPurpur.class,CALLS_REAL_METHODS);Player player=mock(Player.class);
        try(var bukkit=mockStatic(Bukkit.class)){bukkit.when(()->Bukkit.getPluginCommand("afritierslink:join-queue")).thenReturn(null);plugin.join(player,new String[]{"sword"});verify(player).sendMessage(any(net.kyori.adventure.text.Component.class));}
    }
    @Test void offlineUnauthenticatedSessionCannotPollPrivateTickets(){
        var main=mock(AfriTiersPurpur.class);var manager=mock(PluginManager.class);Player player=mock(Player.class);when(player.isOnline()).thenReturn(true);
        try(var bukkit=mockStatic(Bukkit.class)){bukkit.when(Bukkit::getPluginManager).thenReturn(manager);bukkit.when(Bukkit::getOnlineMode).thenReturn(false);TicketService service=new TicketService(main);assertFalse(service.authenticated(player));service.close();}
    }
    @Test void settingsPersistAndRankingControlsAreIndependent()throws Exception{
        var store=new Preferences.Store(directory);UUID player=UUID.randomUUID();SettingsCommand.change(store.get(player),new String[]{"rank-numbers","off"});SettingsCommand.change(store.get(player),new String[]{"color","HT1","#abc"});store.save(player);
        var saved=new Preferences.Store(directory).get(player);assertFalse(saved.rankNumbers);assertTrue(saved.rankTrophies);assertTrue(saved.podiumColors);assertEquals(0xAABBCC,saved.colors.get("HT1"));
    }
    @Test void diavanillaUsesTheDedicatedUntintedIconAndOverallPosition(){
        Rankings rankings=new Rankings();rankings.update("{\"players\":[{\"uuid\":\"00112233445566778899aabbccddeeff\",\"name\":\"Zulu\",\"totalPoints\":10,\"tiers\":[{\"mode\":\"diavanilla\",\"level\":\"HT3\",\"retired\":false}]}]}");
        Preferences prefs=new Preferences();prefs.gameMode="diavanilla";var tag=rankings.tag(rankings.all().getFirst(),prefs,true,null);assertTrue(tag.toString().contains("\uE814"));assertFalse(tag.toString().contains("\uE708"));assertTrue(tag.toString().contains("#1"));
    }
}
