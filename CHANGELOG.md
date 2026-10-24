# Afri-TiersTagger changelog

Changes made during the Afri-Tiers customization of the original Tier Tagger mod, through client 2.7.1, Velocity 1.0.1 and Purpur 1.0.0.

The original mod already provided tier labels, nametag/tab display, player search and a settings foundation. The entries below describe the Afri-Tiers additions, adaptations and fixes. Earlier customization work is grouped where a separate release number was not recorded.

## 2.7.1 — Open-queue completion and DiaVanilla artwork

- `/join-queue ` tab completion lists only queues currently marked open in the public queue feed, filters as you type and excludes closed modes.
- Added the supplied DiaVanilla artwork as its own icon, preserving its blue and white colors instead of sharing the Vanilla icon.
- Included the DiaVanilla icon in every supported client build and in the optional server icon resource pack.
- Retained the existing verified Minecraft-to-Discord queue flow. A queue request uses the actual connected Minecraft player; the backend resolves their linked Discord account and applies eligibility checks.
- Added regression checks for open/closed suggestions, canonical mode aliases and the dedicated DiaVanilla icon.

## Purpur 1.0.0 — New server plugin

- Added a Java 21 plugin targeting Purpur 1.21.11, using PacketEvents 2.14.0.
- Added tier nametags and tab-list labels for players without the client mod, with overall positions, podium colors and optional custom icons.
- Added `/afritiers-settings`, a clickable settings menu with per-player saved preferences.
- Included independent controls for rank numbers, rank trophies, podium colors, gamemode icons, selected mode, fallback tiers, retired tiers, colors and kit identification.
- Added `/afritiers <username>` public profile lookup. Gamemode rows show tiers without repeated placement numbers.
- Added waitlist and personal testing-ticket alerts with Chat/Title and sound controls.
- Added `/join-queue <mode>` forwarding to AfriTiersLink and open-queue tab completion, including the existing Link command when it owns the root command.
- Used the server's synchronized inventory and equipment for kit identification.
- Preserved existing team formatting and visibility rules, respected hidden players and restored display state when disabled.
- Detected client-mod users to avoid duplicate tags and alerts.
- Added optional HTTPS resource-pack delivery, with text labels until the pack loads successfully.

Queue joins and private ticket notices require a configured AfriTiersLink 1.1.0 or newer. Server deployment and a real linked-player queue join must be checked on the target Minecraft server.

## Velocity 1.0.1

- Added open-queue tab completion and the dedicated DiaVanilla glyph.
- Updated the shared optional icon pack.

## 2.7.0 — Minecraft versions and ranking preferences

- Added separate client builds for Minecraft 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and 26.3.
- Added a Java 25 settings implementation for the 26.x clients and version-specific API adaptations.
- Added `/afritiers-settings` as a direct client settings command.
- Added independent switches for overall rank numbers, rank trophy icons and gold/silver/bronze podium colors. These remain separate from gamemode icons.
- Made all rank numbers gray when podium colors are disabled.
- Removed placement numbers and trophies beside gamemodes in profile-search results while preserving tier information.
- Added automated builds for all six clients and the Velocity plugin.

Each client JAR targets its stated Minecraft release. A single JAR does not cover the entire range.

## Velocity 1.0.0 — New proxy plugin

- Added a Java 21 plugin for Velocity 3.5.1 and Minecraft 1.21.11 clients, using PacketEvents 2.14.0.
- Brought tier labels, overall placements, settings, profile lookup, kit identification and queue alerts to players without the client mod.
- Added `/afritiers-settings` with a clickable per-player menu and saved preferences.
- Added optional gamemode/trophy artwork through a resource pack, with a text fallback.
- Forwarded queue joins through the current backend's AfriTiersLink command using the real player session.
- Accepted private ticket notices from the player's current backend and rejected client-injected notices.
- Preserved backend team visibility and avoided duplicate displays for mod users.

## 2.6.1 — Overall ranking correction

- Changed displayed positions to the overall All Modes leaderboard rather than a separate position for each gamemode.
- Kept the same overall position when switching displayed gamemodes.
- Preserved the website's source order for equal total-point scores.
- Kept #1 gold, #2 silver, #3 bronze and #4 onward gray.

## 2.6.0 — In-game joins, alerts and ranking display

- Changed `/join-queue <gamemode>` from opening Discord to requesting a queue join through AfriTiersLink 1.1.0 on a supported server.
- Reused the existing verified Minecraft-to-Discord link, without requiring a new Discord sign-in.
- Added clear unlinked-account guidance and normal backend success/error responses.
- Added private testing-ticket notifications addressed to the matching player.
- Added a dedicated Alerts tab with separate waitlist/ticket enable switches, Chat or Title placement and sound controls.
- Added a single-note ticket sound while retaining the three-second waitlist alert sequence.
- Added overall placement display and top-three trophy artwork; the position calculation was corrected in 2.6.1.
- Added live color swatches beside tier-color inputs.
- Added the Afri-Tiers website and issue-report links to mod metadata.

## 2.5.0 and earlier Afri-Tiers customization

- Adapted the mod to Afri-Tiers branding and public ranking data.
- Used current tier placements, points and retirement status from the public Afri-Tiers API.
- Added the Afri-Tiers gamemode names, aliases and supplied icon artwork.
- Kept familiar nametag, tab-list, profile-search, tier-color and fallback settings working with Afri-Tiers data.
- Removed `/afritiertester` and privileged staff API functionality from the public client.
- Removed legacy staff credentials when saving configuration and kept private bot credentials out of the public source and mod.
- Added public queue polling every five seconds and alerts when a queue newly opens.
- Added queue-event deduplication so initial loading, duplicate responses, another tester joining and bot restarts do not replay old openings.
- Queued simultaneous openings for sequential display.
- Added the original `/join-queue <gamemode>` shortcut to the corresponding Discord waitlist channel; this behavior was replaced by the linked in-game flow in 2.6.0.
- Added the optional Kit Identifier setting, off by default, using observed armor, held items and the local player's synchronized inventory.
- Added kit signatures and ambiguity handling, falling back to the configured mode when evidence is incomplete or inconclusive.
- Kept kit observations temporary and local. The client does not claim to inspect other players' hidden inventories.
- Added tests for kit rules, incomplete observations, ranking parsing, queue events and bounded HTTP responses.
- Published buildable public source with the original MPL-2.0 license and attribution retained.

## Companion integration changes

These support the mod but are changes to the backend or website:

- AfriTiersLink 1.1.0 authenticates server requests, verifies the Minecraft session, resolves the existing Discord account link and applies queue restrictions, cooldowns, capacity and existing-ticket checks.
- Queue membership continues to use the normal Discord account and its existing name/mention presentation.
- New automatic testing cooldowns use five days, while existing expiry timestamps and already-running legacy HT3 cooldowns remain intact.
- Minecraft usernames refresh by UUID. Premium website portraits use Visage busts; cracked players use the supplied fallback image.
- Website kit information was expanded with the supplied inventory previews, LT/HT variants, item artwork, quantities, enchantments and restrictions.

## Validation and installation status

The previous 2.7.0 release passed builds, tests and actual client startup checks for all six Minecraft versions. The updated 2.7.1 and plugin validation results are listed in the accompanying release notes. Purpur server deployment and an actual authenticated queue join were not performed on a production Minecraft server.
