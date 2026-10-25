# Afri-TiersTagger client changelog

Changes added during the Afri-Tiers customization of the original Tier Tagger mod, through version 2.8.3. The original mod already provided tier labels, nametag/tab display, player search and a settings foundation. Earlier customization is grouped where a separate release number was not recorded.

## 2.8.3 — Smaller profile portraits

- Capped the profile bust at the plugin's 96 × 96 GUI-pixel size instead of enlarging it with the window.
- Centered the portrait and stats together in a compact profile area.

## 2.8.2 — Compact ranking buttons

- Resized player ranking buttons to the plugin's 310 × 20 GUI-pixel dimensions, centered instead of stretching across the screen.
- Fitted bust thumbnails inside the shorter rows and aligned the search controls with the compact list.
- Matched the profile's Back to rankings button to the plugin's 150 × 20 dimensions.

## 2.8.1 — Ranking portraits and required icons

- Added the website's half-body bust thumbnails inside clickable player ranking buttons.
- Added untinted gold, silver and bronze trophy icons to overall #1, #2 and #3 in rankings and profile summaries.
- Kept the full website bust in player profiles, with the supplied fallback for cracked accounts.
- Made gamemode and trophy icons always enabled, including when loading older settings with icons disabled. Rank number and podium color preferences remain available.
- Removed icon-disable controls from both client settings screens.
- Release downloads contain only the mod and its source.

## 2.8.0 — In-game rankings and website portraits

- Added an in-game rankings browser with Overall and all supported gamemode tabs, username search, paging and player profiles.
- Added `/afritiers` and `/afritiers <username>` client commands. The existing Search Player setting opens the browser too.
- Replaced the profile's locally resolved 3D player with the website's exact Visage bust PNG. Cracked and unknown accounts use the supplied website fallback image.
- Kept Overall positions unchanged when switching modes, with tier-based ordering within a selected mode and active tiers ahead of retired tiers.
- Kept profile gamemode rows free of repeated ranking numbers.
- Added bounded asynchronous image downloads, dimension validation, a small expiring portrait cache and texture cleanup when closing a profile.
- Added the Afri-Tiers copyright notice while preserving original MCTiers attribution and MPL-2.0 terms.

## 2.7.1 — Open-queue completion and DiaVanilla artwork

- `/join-queue ` tab completion lists only queues currently marked open in the public queue feed, filters as you type and excludes closed modes.
- Added the supplied DiaVanilla artwork as its own icon, preserving its blue and white colors instead of sharing the Vanilla icon.
- Included the DiaVanilla icon in every supported client build.
- Retained the existing verified Minecraft-to-Discord queue flow. A queue request uses the actual connected Minecraft player; the backend resolves their linked Discord account and applies eligibility checks.
- Added regression checks for open/closed suggestions, canonical mode aliases and the dedicated DiaVanilla icon.
- Restored the live bot queue-feed route and opening-event recording after the current bot source had lost that integration. This repaired the HTTP 503 feed failure that prevented in-game queue-open alerts. The existing mod benefits from this server-side repair without an update.

## 2.7.0 — Minecraft versions and ranking preferences

- Added separate client builds for Minecraft 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and 26.3.
- Added a Java 25 settings implementation for the 26.x clients and version-specific API adaptations.
- Added `/afritiers-settings` as a direct client settings command.
- Added independent switches for overall rank numbers, rank trophy icons and gold/silver/bronze podium colors. These remain separate from gamemode icons.
- Made all rank numbers gray when podium colors are disabled.
- Removed placement numbers and trophies beside gamemodes in profile-search results while preserving tier information.
- Added automated builds for all six Minecraft client versions.

Each client JAR targets its stated Minecraft release. A single JAR does not cover the entire range.

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

## Validation

All six client builds passed 76 tests per version and actual Minecraft startup checks, including the dedicated DiaVanilla texture, rank settings, colors and profile rows. The public queue feed returned HTTP 200 after its backend repair. A real linked-player queue join was not manually tested in this release.
