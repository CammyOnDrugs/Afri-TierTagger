# Afri-TiersTagger

Fabric client mod for Minecraft **1.21.11**, maintained by **Brianni_MC (Afri-Tiers)**.

Displays Afri-Tiers rankings beside player names, with the existing tier colours, icons, player search and settings. Rankings load from the public Afri-Tiers website.

## Install

Use Java 21, Fabric Loader 0.18.4 or newer, Fabric API for 1.21.11 and Ukulib 1.10.2 for 1.21.11. Remove the previous Afri-TiersTagger JAR before adding the updated JAR. Keep only one version installed.

Open the existing Afri-TiersTagger settings through Ukulib/Mod Menu or the configured settings key. Existing display settings are retained. Legacy staff credentials are ignored and removed when the configuration is saved.

## Queue notifications

While in a world, the mod checks the public queue status every five seconds. A newly opened queue displays the red title and white subtitle, with repeated noteblock sounds for three seconds. Initial loading, duplicate responses, a second tester joining and bot restarts do not repeat old alerts. Simultaneous openings are displayed in sequence.

`/join-queue <gamemode>` opens that mode's public Discord waitlist channel. Click its **Join Queue** button there; Discord verifies your account and eligibility. This client does not bypass Discord authentication or submit unauthenticated queue changes.

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

This repository contains client source, build files, tests and public artwork only. No bot source, Discord bot token, API key, database, player/account export, private backend address or workstation configuration is required to build or use it. The client uses public HTTPS GET endpoints only. Publishing this code does not grant access to private service operations.

Derived from Tier Tagger; original copyright and MPL-2.0 terms are preserved in [LICENSE](LICENSE) and [NOTICE](NOTICE).
