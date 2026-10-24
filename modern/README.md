# Afri-TiersTagger 2.7.1 for Minecraft 26.x

Separate builds support **26.1, 26.1.1, 26.1.2, 26.2 and 26.3**. Use Java 25, Fabric Loader 0.19.5 or newer and Fabric API for your exact Minecraft release. Ukulib is not required by these builds.

Use the JAR matching your Minecraft version. Open settings using **`/afritiers-settings`** or the settings key (Insert by default). General settings include independent switches for overall rank numbers, trophy icons and podium colors, alongside gamemode icons, kit identification, selected mode, retired tiers and highest-tier fallback. Colors and Alerts have their own tabs.

Overall rank is the same across gamemodes. Numbers are gold for #1, silver for #2, bronze for #3 and gray thereafter when podium colors are enabled. Profile-search gamemode rows show tiers without placement numbers or trophies.

Queue joining and private ticket alerts use AfriTiersLink 1.1.0 or newer on the current Minecraft backend. The existing Discord link is checked by the server; no additional Discord sign-in is needed.

Build with JDK 25:

```sh
./gradlew -Pminecraft_version=26.3 build
```

Replace `26.3` with another supported release. The matching Fabric API version is selected automatically. On Windows use `gradlew.bat`. `version-source.gradle` applies the tested API differences to generated sources under `build/`; the maintained sources remain under `src/main/java`. Each output declares its exact Minecraft version.

The 1.21.11 build is maintained at the repository root and uses Java 21 with Ukulib. See the root README, LICENSE and NOTICE.

## Latest additions

`/join-queue ` completes only currently open queues. DiaVanilla uses its supplied dedicated icon. The [full changelog](../CHANGELOG.md) records Afri-Tiers customization since the original mod. A [Purpur 1.21.11 plugin](../purpur/) is also available with `/afritiers-settings`.
