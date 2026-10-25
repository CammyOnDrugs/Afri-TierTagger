# Afri-TiersTagger 2.8.2

Fabric client mod for Minecraft 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and 26.3. Install only the JAR matching your exact game version.

## New in this release

- Player ranking buttons now match the plugin's 310 × 20 GUI-pixel size instead of stretching across the screen.
- Thumbnails fit inside the shorter buttons; the search controls align with the compact list.
- The profile's Back to rankings button uses the plugin's 150 × 20 size.

This update changes interface dimensions and spacing only.

## Installation

| Minecraft | Java | Dependencies |
| --- | --- | --- |
| 1.21.11 | 21 | Fabric Loader 0.18.4+, matching Fabric API, Ukulib 1.10.2 |
| 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 25 | Fabric Loader 0.19.5+, matching Fabric API |

The 26.x versions do not require Ukulib. Replace the old Afri-TiersTagger JAR and restart Minecraft. Keep one Afri-TiersTagger JAR installed. Display settings remain available at `/afritiers-settings`; the Search Player setting opens the browser.

Existing queue completion, linked-account joining, alerts, kit identification and rank number and podium color settings are retained. Queue joining requires support from the Minecraft server. This download contains the client mod and its source; server plugins are excluded.

## Validation

The release workflow builds and tests all six Minecraft versions. Local Minecraft render probes check ranking button thumbnails and both premium and cracked profile portraits. Server plugins are excluded from this release.
