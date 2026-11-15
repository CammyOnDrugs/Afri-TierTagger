package com.skalpha.tiertagger.config;
import com.google.gson.JsonParser;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class UpdatesTest {
    @TempDir Path directory;
    @Test void selectsOnlyANewerVerifiedAssetForTheExactMinecraftVersion(){
        var release=JsonParser.parseString("{\"draft\":false,\"prerelease\":false,\"tag_name\":\"v2.9.0\",\"assets\":[{\"name\":\"Afri-TiersTagger-2.9.0+mc26.1.1.jar\",\"digest\":\"sha256:"+"0".repeat(64)+"\",\"browser_download_url\":\"https://github.com/CammyOnDrugs/Afri-TierTagger/releases/download/v2.9.0/mod.jar\"}]}").getAsJsonObject();
        assertNull(Updates.selectGitHub(release,"26.1","2.8.5"));assertNotNull(Updates.selectGitHub(release,"26.1.1","2.8.5"));assertNull(Updates.selectGitHub(release,"26.1.1","3.0.0"));
        release.getAsJsonArray("assets").get(0).getAsJsonObject().addProperty("digest","sha256:invalid");assertNull(Updates.selectGitHub(release,"26.1.1","2.8.5"));
        assertFalse(Updates.newer("2.8.4","2.8.5+mc26.1"));assertTrue(Updates.newer("2.10.0","2.9.9"));
    }
    @Test void modifiedFilesAreRejectedAndSuccessfulInstallKeepsABackup()throws Exception{
        Path mods=Files.createDirectories(directory.resolve("mods")),target=mods.resolve("mod.jar"),pending=directory.resolve("update.pending");Files.writeString(target,"old");Files.writeString(pending,"new");String original=UpdateInstaller.hash(target),replacement=UpdateInstaller.hash(pending);
        assertThrows(IllegalArgumentException.class,()->UpdateInstaller.install(target,pending,"0".repeat(64),replacement));assertEquals("old",Files.readString(target));assertEquals("new",Files.readString(pending));
        UpdateInstaller.install(target,pending,original,replacement);assertEquals("new",Files.readString(target));assertFalse(Files.exists(pending));try(var files=Files.list(mods)){assertEquals(1,files.filter(path->path.toString().endsWith(".afritiers-backup")).count());}
    }
    @Test void checksumMismatchStopsTheDownload()throws Exception{
        var candidate=new Updates.Candidate("2.9.0","https://github.com/CammyOnDrugs/Afri-TierTagger/releases/download/v2.9.0/mod.jar","SHA-256","0".repeat(64));assertThrows(java.io.IOException.class,()->Updates.verify(new byte[]{1,2,3},candidate));
    }
}
