# Afri-TiersTagger 2.7.1

Fabric client mod for Minecraft 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and 26.3. Install only the JAR matching your exact Minecraft version.

## Changes

- `/join-queue ` Tab completion suggests currently open queues and filters as you type.
- DiaVanilla has its own supplied blue and white icon.
- Rankings use the overall All Modes position across gamemodes. #1 is gold, #2 silver, #3 bronze and later positions gray.
- Rank numbers, trophy icons and podium colors have independent client settings. Profile-search gamemode rows show tiers without repeated ranking numbers.
- Queue-open alerts use the repaired public feed. Already-open queues appear in completion; old opening alerts are not replayed when joining a world.
- Existing linked Minecraft accounts join with their linked Discord queue identity on supported servers, without a new Discord sign-in.
- Kit identification uses observed equipment and your synchronized inventory, with the configured fallback when evidence is incomplete.

## Installation

| Minecraft | Java | Dependencies |
| --- | --- | --- |
| 1.21.11 | 21 | Fabric Loader 0.18.4+, matching Fabric API, Ukulib 1.10.2 |
| 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 25 | Fabric Loader 0.19.5+, Fabric API for your exact game version |

The 26.x builds do not require Ukulib. Replace the old Afri-TiersTagger JAR, keep only one version in mods, then restart Minecraft. Open settings with `/afritiers-settings`.

This release contains the client mod and its source. The source ZIP, changelog and SHA256SUMS are included alongside all six JARs and the combined download.

All six versions passed 76 tests per version and actual Minecraft startup checks. The published JARs come from the successful six-client GitHub Actions build in run 40137934731. A real linked-player queue join was not manually tested in this release.

See CHANGELOG.md for the client customization history. MPL-2.0 license and original attribution are preserved.
