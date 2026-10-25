# Afri-TiersTagger

Fabric client mod for Minecraft **1.21.11 through 26.3**, maintained by **Brianni_MC (Afri-Tiers)**.

Displays Afri-Tiers rankings beside player names, with the existing tier colours, icons, player search and settings. Rankings load from the public Afri-Tiers website.

## Versions

| Minecraft | Project | Java | Additional requirements |
| --- | --- | --- | --- |
| 1.21.11 | Repository root | 21 | Fabric API for 1.21.11, Ukulib 1.10.2 |
| 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | [modern](modern/) | 25 | Fabric API for the exact Minecraft release |

Version **2.8.1** adds bust thumbnails to ranking buttons and top-three trophies to rankings and profile summaries. Gamemode and trophy icons stay enabled. Overall rank numbers and gold/silver/bronze podium colors retain their switches. Turning podium colors off makes all numbers gray. Profile-search gamemode rows show tier information without placement numbers or trophies. Each Minecraft build declares its exact game version; choose one matching your client.

Open the client settings screen with **`/afritiers-settings`**.

## Install for Minecraft 1.21.11

Use Java 21, Fabric Loader 0.18.4 or newer, Fabric API for 1.21.11 and Ukulib 1.10.2 for 1.21.11. Remove the previous Afri-TiersTagger JAR before adding the updated JAR. Keep only one version installed.

Open the existing Afri-TiersTagger settings through Ukulib/Mod Menu or the configured settings key. Existing display settings are retained. Legacy staff credentials are ignored and removed when the configuration is saved.

## Queue notifications

While in a world, the mod checks the public queue status every five seconds. A newly opened queue displays the red title and white subtitle, with repeated noteblock sounds for three seconds. Initial loading, duplicate responses, a second tester joining and bot restarts do not repeat old alerts. Simultaneous openings are displayed in sequence.

`/join-queue <gamemode>` joins through AfriTiersLink 1.1.0 on LA-Network or SurgeMC. The server identifies the actual online Minecraft player, and the bot resolves the existing verified Discord link before applying the normal restrictions, cooldown and queue checks. No Discord sign-in is required. Unlinked players are directed to `/lobby` and `/afritiers-link`. Other servers need the supported plugin before they can forward queue requests.

The **Alerts** tab separately controls waitlist and testing-ticket notifications, Chat or Title placement, and sounds. Ticket notifications play one note. Waitlist alerts retain the original three-second sound sequence when enabled.

The player's overall All Modes leaderboard position appears in nametags and the tab list, with the same position across gamemodes and trophies for the overall top three. The number is gold for #1, silver for #2, bronze for #3 and gray for #4 onward. Positions follow total points and the website's order for tied scores. Tier colour inputs show a live colour swatch. Website and issue links appear in mod metadata.

The staff command `/afritiertester` and all staff API functionality have been removed. `/afritiersreload` and `/tiertagger` remain available.

## Kit Identifier

The **Kit Identifier** switch in settings is off by default. When enabled, the mod uses a matching observed kit to select that player's displayed gamemode tier. Missing, ambiguous or unavailable evidence falls back to the configured gamemode and its existing fallback setting.

Your own synchronized inventory is available. Other players expose only their worn armor and held items; Minecraft does not send their hidden inventories to clients. Consequently, some kits cannot be identified for other players. Kit detection never invents hidden item counts or treats unseen items as absent. Inventory observations are temporary, local and never uploaded.

## Build and test

With JDK 21 installed:

```sh
./gradlew build
```

On Windows use `gradlew.bat build`. The remapped mod and source JARs are written to `build/libs/`. Tests cover kit signatures, incomplete observations, ambiguity, current rankings parsing and queue-event deduplication.

## Public source boundaries

This repository contains client source, build files, tests and public artwork only. No bot source, Discord bot token, API key, database, player/account export, private backend address or workstation configuration is required to build or use it. The client uses public HTTPS GET endpoints and sends its queue command to the current Minecraft server. It contains no private queue API credentials. Publishing this code does not grant access to private service operations.

Derived from Tier Tagger; original copyright and MPL-2.0 terms are preserved in [LICENSE](LICENSE) and [NOTICE](NOTICE).

Copyright (c) 2026 Brianni_MC (Afri-Tiers) for Afri-Tiers modifications and original contributions. See [COPYRIGHT](COPYRIGHT) for attribution and third-party ownership.

## Latest release

[Download 2.8.1](https://github.com/CammyOnDrugs/Afri-TierTagger/releases/tag/v2.8.1) for your exact Minecraft version. The release includes all six client JARs, a combined download, mod-only source, changelog and SHA-256 checksums. See [release notes](RELEASE-NOTES.md) and the [client changelog](CHANGELOG.md).

`/join-queue ` completes currently open queues. DiaVanilla uses its supplied dedicated icon. This repository and its releases contain the client mod.


## In-game rankings and profiles

Use `/afritiers` to browse Overall rankings, choose gamemode tabs at the top, search Minecraft usernames and open player profiles. `/afritiers <username>` opens a cached matching profile directly. The settings Search Player button opens the same browser.

Profile portraits use the same Visage bust URL as the website for premium accounts and the same supplied fallback PNG for cracked/unknown accounts. Images load asynchronously, fit their original aspect ratio and are cached briefly while browsing. Overall placement stays consistent across gamemodes; profile tier rows show tier information without repeated placement numbers.
