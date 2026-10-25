# Afri-TiersTagger 2.8.0

Fabric client mod for Minecraft 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and 26.3. Install only the JAR matching your exact game version.

## New in this release

- `/afritiers` opens the in-game rankings browser with Overall and gamemode tabs at the top, username search and paging.
- Select a player to open their profile. `/afritiers <username>` opens a cached matching profile directly.
- Profile portraits display the website's full Visage bust PNG for premium accounts and your supplied fallback image for cracked/unknown accounts.
- Switch gamemode tabs to browse tiers while retaining each player's overall position. Tier rows in profiles do not repeat ranking numbers.
- Images load asynchronously with bounded downloads, dimension checks, a small expiring cache and texture cleanup.
- Added Afri-Tiers copyright attribution while preserving the original MPL-2.0 license.

## Installation

| Minecraft | Java | Dependencies |
| --- | --- | --- |
| 1.21.11 | 21 | Fabric Loader 0.18.4+, matching Fabric API, Ukulib 1.10.2 |
| 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 25 | Fabric Loader 0.19.5+, matching Fabric API |

The 26.x versions do not require Ukulib. Replace the old Afri-TiersTagger JAR and restart Minecraft. Keep one Afri-TiersTagger JAR installed. Display settings remain available at `/afritiers-settings`; the Search Player setting opens the browser.

Existing queue completion, linked-account joining, alerts, kit identification and independent rank display settings are retained. Queue joining requires support from the Minecraft server. This download contains the client mod and its source; server plugins are excluded.
