package net.afritiers.purpur;
import io.papermc.paper.dialog.DialogResponseView;
import java.util.*;
import org.bukkit.packs.ResourcePack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DialogSettingsTest {
    @Test void nativeUnderscoreFieldNamesMapBackToSavedSettingKeys(){var view=mock(DialogResponseView.class);for(var toggle:SettingsDialogs.GENERAL)when(view.getBoolean(toggle.key().replace('-','_'))).thenReturn(true);when(view.getBoolean("rank_numbers")).thenReturn(false);when(view.getText("mode")).thenReturn("sword");when(view.getText("highest")).thenReturn("never");var p=new Preferences();SettingsDialogs.apply(p,"general",SettingsDialogs.read("general",view));assertFalse(p.rankNumbers);assertTrue(p.rankTrophies);assertEquals("sword",p.gameMode);assertEquals("never",p.highest);}

    private Map<String,String> general(){Map<String,String> values=new LinkedHashMap<>();for(var toggle:SettingsDialogs.GENERAL)values.put(toggle.key(),"on");values.put("mode","sword");values.put("highest","not_found");return values;}
    @Test void dialogGeneralAppliesAllControlsAndKeepsRankingSwitchesIndependent(){var p=new Preferences();var values=general();values.put("rank-numbers","off");values.put("icons","off");SettingsDialogs.apply(p,"general",values);assertEquals("sword",p.gameMode);assertFalse(p.rankNumbers);assertFalse(p.icons);assertTrue(p.rankTrophies);assertTrue(p.podiumColors);}
    @Test void invalidModeCannotPartiallyApplyEarlierCheckboxes(){var p=new Preferences();var values=general();values.put("enabled","off");values.put("mode","fake-mode");assertThrows(IllegalArgumentException.class,()->SettingsDialogs.apply(p,"general",values));assertTrue(p.enabled);assertEquals("overall",p.gameMode);}
    @Test void malformedColorsCannotPartiallyReplaceValidColors(){var p=new Preferences();var before=new HashMap<>(p.colors);Map<String,String> values=new LinkedHashMap<>();SettingsDialogs.COLORS.forEach(key->values.put(key,"#123456"));values.put("retired","oops");assertThrows(IllegalArgumentException.class,()->SettingsDialogs.apply(p,"colors",values));assertEquals(before,p.colors);}
    @Test void missingControlsAreRejectedWithoutChangingPreferences(){var p=new Preferences();var values=general();values.remove("tab");assertThrows(IllegalArgumentException.class,()->SettingsDialogs.apply(p,"general",values));assertTrue(p.playerList);}
    @Test void colorDialogAcceptsShortHexAndUpdatesEveryTier(){var p=new Preferences();Map<String,String> values=new HashMap<>();SettingsDialogs.COLORS.forEach(key->values.put(key,"#abc"));SettingsDialogs.apply(p,"colors",values);assertTrue(p.colors.values().stream().allMatch(c->c==0xAABBCC));}
    @Test void alertDialogSavesLocationsAndIndependentSounds(){var p=new Preferences();Map<String,String> values=new HashMap<>();SettingsDialogs.ALERTS.forEach(toggle->values.put(toggle.key(),"on"));values.put("waitlist-sound","off");values.put("waitlist-location","chat");values.put("ticket-location","title");SettingsDialogs.apply(p,"alerts",values);assertFalse(p.waitlistSound);assertTrue(p.ticketSound);assertEquals("chat",p.waitlistLocation);assertEquals("title",p.ticketLocation);}
    @Test void inputReadsRequireTypedBooleanValues(){var view=mock(DialogResponseView.class);when(view.getBoolean(anyString())).thenReturn(null);assertThrows(IllegalArgumentException.class,()->SettingsDialogs.read("general",view));}
    @Test void configuredServerPackIsRecognizedByExpectedHash(){var pack=mock(ResourcePack.class);when(pack.getId()).thenReturn(UUID.randomUUID());when(pack.getHash()).thenReturn(AfriTiersPurpur.ICON_SHA1.toUpperCase(Locale.ROOT));assertTrue(AfriTiersPurpur.serverIcons(pack,AfriTiersPurpur.ICON_SHA1));when(pack.getHash()).thenReturn("a".repeat(40));assertFalse(AfriTiersPurpur.serverIcons(pack,AfriTiersPurpur.ICON_SHA1));assertFalse(AfriTiersPurpur.serverIcons(null,AfriTiersPurpur.ICON_SHA1));}
}
