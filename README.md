# Afri-TiersTagger

Fabric client mod showing Afri-Tiers PvP tiers beside player name tags and in the tab list.

## Version 2.8.6

Supports Minecraft 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and 26.3. Download the matching JAR from Releases. Requires Fabric Loader 0.19.5+, Fabric API, and Ukulib on 1.21.11. WalksyLib 2.0.5 is bundled. Remove the old mod JAR before installing.

Four icon styles cover all 18 modes. The mod includes queue menus, appearance controls, separate name-tag/tab switches, own-tier-in-F5 display and checksum-verified updates. Minecraft style disables Liquid Glass. Holograms are not tagged. Version 2.8.6 is not obfuscated.

## Build

Run `python3 tools/fetch-dependencies.py` to fetch the pinned, SHA-512-verified WalksyLib and compile-only Mod Menu dependencies.

- JDK 21: `./gradlew build` in the repository root builds 1.21.11.
- JDK 25: `cd modern && ./gradlew -Pminecraft_version=26.3 build` builds a selected 26.x version. Supported values: 26.1, 26.1.1, 26.1.2, 26.2 and 26.3.

All six local Gradle builds passed. In-game runtime checks for changed 2.8.6 code and authenticated multiplayer checks remain outstanding. See RELEASE-NOTES.md for validation limits.

## Licence

MPL-2.0 for the mod. See LICENSE, COPYRIGHT, NOTICE and THIRD_PARTY_WALKSY.txt. WalksyLib attribution and licence obligations remain unchanged.
