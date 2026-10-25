# Afri-TiersTagger 2.8.3

Fabric client mod for Minecraft 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and 26.3. Install only the JAR matching your exact game version.

## New in this release

- Profile portraits use a maximum 96 × 96 GUI-pixel box matching the plugin, instead of expanding across half the screen.
- Portraits and player stats sit together in a compact centered profile area.

The existing compact ranking buttons, tabs, portraits, tier colors and commands are retained.

## Installation

| Minecraft | Java | Dependencies |
| --- | --- | --- |
| 1.21.11 | 21 | Fabric Loader 0.18.4+, matching Fabric API, Ukulib 1.10.2 |
| 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 25 | Fabric Loader 0.19.5+, matching Fabric API |

The 26.x versions do not require Ukulib. Replace the old Afri-TiersTagger JAR and restart Minecraft. Keep one Afri-TiersTagger JAR installed. Display settings remain available at `/afritiers-settings`; the Search Player setting opens the browser.

Existing queue completion, linked-account joining, alerts, kit identification and rank number and podium color settings are retained. Queue joining requires support from the Minecraft server. This download contains the client mod and its source; server plugins are excluded.

## Validation

The release workflow builds and tests all six Minecraft versions. Local Minecraft render probes check ranking button thumbnails and both premium and cracked profile portraits. Server plugins are excluded from this release.
