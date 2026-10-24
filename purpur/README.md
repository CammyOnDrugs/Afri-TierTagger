# Afri-Tiers Purpur 1.1.1

For Purpur/Paper-compatible Minecraft 1.21.11, Java 21 and PacketEvents Spigot/Paper 2.14.0.

## Install

Stop the server, back up the old plugin outside plugins/, upload AfriTiersPurpur-1.1.1.jar and PacketEvents, then restart. Keep AfriTiersLink 1.1.0 or newer and its existing working configuration/certificate on the same backend for queue joining and personal ticket notices. Use either this display plugin or the Velocity display plugin for a connection.

## Required icons

The Afri-Tiers icon pack is mandatory. Every client, including client-mod users, must load it. Offers use Minecraft's required flag. Declining, failing to load or discarding the last recognised icon pack disconnects the player. A client that never finishes loading is disconnected after a two-minute grace period. The supplied default HTTPS URL and SHA-1 point to the hosted pack included in this bundle.

An existing server.properties pack is recognised when its SHA-1 matches the configured icon pack. Keep require-resource-pack=true when using that pack. If no recognised pack is loaded, the plugin sends its required offer automatically. The resource-pack URL and SHA-1 can be changed by the server operator in plugins/AfriTiersPurpur/config.yml when hosting a compatible pack elsewhere.

Gamemode and rank-trophy icons have no player switches. Old saved opt-outs are ignored and migrated; old icon-setting commands are rejected. Rank-number and podium-color switches remain available. Text is used only while the required pack is still loading.

## Settings and commands

/afritiers-settings opens native Minecraft dialog pages for Display & Rankings, Tier Colors and Alerts. Save validates all inputs before persisting preferences by UUID. Cancel leaves preferences unchanged. If the client mod intercepts the command, use /afritierspurpur:afritiers-settings.

/afritiers <username> searches public tiers. /join-queue <mode> forwards the actual player to AfriTiersLink; Tab lists currently open queues. The existing verified Minecraft-to-Discord link and normal backend eligibility checks are reused.

/afritiersreload reloads this plugin's configuration and verifies signed backend connectivity. It requires afritiers.purpur.reload; the legacy afritiers.reload permission is accepted too. /afritiers-reload belongs to AfriTiersLink.

Kit identification uses synchronized server inventory and equipment. Overall rankings remain the same across gamemodes. Profile mode rows contain tiers without repeated rank numbers. Modded clients keep their client display and alert handling, while mandatory pack enforcement still applies.

## Build

With JDK 21 and Maven, run mvn package. Output: target/AfriTiersPurpur-1.1.1.jar. Purpur and PacketEvents are provided dependencies; Gson is shaded. Tests cover native settings, legacy icon opt-outs, mandatory pack requests and load/failure state transitions, rankings, queue forwarding, authentication and shared kit/queue/HTTP rules.

MPL-2.0; see LICENSE and NOTICE. PacketEvents is installed separately under its own license.
