package net.afritiers.purpur;
import java.nio.file.*;
import java.util.*;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RequiredIconsTest {
    @TempDir Path directory;
    @Test void downloadProgressIsNotAcceptedAsSuccessfulLoading(){var state=new IconPackState();UUID id=UUID.randomUUID();assertEquals(IconPackState.Outcome.WAITING,state.accept(id,Status.ACCEPTED));assertEquals(IconPackState.Outcome.WAITING,state.accept(id,Status.DOWNLOADED));assertEquals(IconPackState.Outcome.READY,state.accept(id,Status.SUCCESSFULLY_LOADED));}
    @Test void decliningOrFailingThePackRejectsTheSession(){for(Status failure:List.of(Status.DECLINED,Status.FAILED_DOWNLOAD,Status.INVALID_URL,Status.FAILED_RELOAD,Status.DISCARDED)){var state=new IconPackState();UUID id=UUID.randomUUID();state.accept(id,Status.ACCEPTED);assertEquals(IconPackState.Outcome.REJECTED,state.accept(id,failure));}}
    @Test void removingTheLastLoadedPackRejectsButAnotherTrustedLoadedPackIsRetained(){var state=new IconPackState();UUID first=UUID.randomUUID(),second=UUID.randomUUID();state.accept(first,Status.SUCCESSFULLY_LOADED);state.accept(second,Status.SUCCESSFULLY_LOADED);assertEquals(IconPackState.Outcome.READY,state.accept(first,Status.DISCARDED));assertEquals(IconPackState.Outcome.REJECTED,state.accept(second,Status.DISCARDED));}
    @Test void playerCannotUseOldCommandsOrDialogFieldsToDisableIcons(){var p=new Preferences();for(String setting:List.of("icons","rank-trophies"))assertThrows(IllegalArgumentException.class,()->SettingsCommand.change(p,new String[]{setting,"off"}));assertTrue(SettingsDialogs.GENERAL.stream().noneMatch(t->Set.of("icons","rank-trophies").contains(t.key())));}
    @Test void oldDisabledPreferencesCannotSuppressGamemodeOrTrophyGlyphs(){Rankings ranks=new Rankings();ranks.update("{\"players\":[{\"uuid\":\"00112233445566778899aabbccddeeff\",\"name\":\"Example\",\"totalPoints\":10,\"tiers\":[{\"mode\":\"sword\",\"level\":\"LT3\"}]}]}");var p=new Preferences();p.icons=false;p.rankTrophies=false;p.rankNumbers=false;String tag=ranks.tag(ranks.all().getFirst(),p,true,null).toString();assertTrue(tag.contains("\uE706"));assertTrue(tag.contains("\uE901"));assertFalse(tag.contains("#1"));}
    @Test void savedLegacyOptOutIsMigratedWithoutLosingOtherSettings()throws Exception{UUID id=UUID.randomUUID();Files.createDirectories(directory.resolve("players"));Files.writeString(directory.resolve("players/"+id+".json"),"{\"icons\":false,\"rankTrophies\":false,\"rankNumbers\":false}");var prefs=new Preferences.Store(directory).get(id);assertTrue(prefs.icons);assertTrue(prefs.rankTrophies);assertFalse(prefs.rankNumbers);}
    @Test void pluginOfferUsesMinecraftRequiredFlag()throws Exception{var plugin=mock(AfriTiersPurpur.class,CALLS_REAL_METHODS);var url=AfriTiersPurpur.class.getDeclaredField("packUrl");url.setAccessible(true);url.set(plugin,AfriTiersPurpur.ICON_URL);var hash=AfriTiersPurpur.class.getDeclaredField("packSha1");hash.setAccessible(true);hash.set(plugin,AfriTiersPurpur.ICON_SHA1);Player player=mock(Player.class);plugin.offerIcons(player);verify(player).setResourcePack(any(UUID.class),eq(AfriTiersPurpur.ICON_URL),any(byte[].class),any(net.kyori.adventure.text.Component.class),eq(true));}
}
