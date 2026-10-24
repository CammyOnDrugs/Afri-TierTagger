# Afri-Tiers Purpur 1.0.0

For **Purpur 1.21.11**, Java 21 and PacketEvents **2.14.0**. Provides tier nametags, tab labels, overall placements, profile lookup, kit identification and queue alerts for players without the client mod.

## Install

1. Put `AfriTiersPurpur-1.0.0.jar` and the Spigot/Paper build of PacketEvents 2.14.0 in the server's `plugins` folder.
2. Keep AfriTiersLink 1.1.0 or newer installed with its working configuration and backend certificate for queue joining and personal ticket notifications.
3. Restart the server. Run `/afritiers-settings` in-game to open the clickable settings menu.

Use one server display integration on a connection. If the proxy runs AfriTiersVelocity, use that display integration instead of also installing AfriTiersPurpur on its backend. AfriTiersLink remains the queue/authentication bridge in either setup.

## Commands and settings

- `/afritiers-settings`: per-player preferences, saved by UUID.
- `/afritiers <Minecraft username>`: public profile lookup; gamemode rows contain tiers without repeated ranking numbers.
- `/join-queue <gamemode>`: forwards the real player to the existing AfriTiersLink command. Press Tab after the space to list queues currently marked open in the public feed. The feed refreshes every five seconds; the backend makes the final queue-open and eligibility checks.
- `/afritiersreload`: refresh rankings and reload server settings; requires `afritiers.reload`.

Preferences include enabled, nametags, tab list, selected mode, highest-tier fallback, retired tiers, mode icons, kit identification, tier colors and waitlist/ticket Chat/Title/sound controls. Overall rank numbers, trophy icons and podium colors have independent switches. Positions use All Modes overall ranking. #1 is gold, #2 silver, #3 bronze and #4 onward gray. Turning podium colors off makes every number gray.

Kit detection reads the server's synchronized inventory, armor and offhand. Ambiguous kits fall back to the selected mode. Observations remain in server memory. Existing scoreboard-team formatting and visibility are preserved. Players using the Afri-Tiers client mod retain their client display and alerts to avoid duplicates.

## Optional icon pack

Host `AfriTiers-server-icons-mc1.21.11.zip` at a direct public HTTPS download URL. Set `resource-pack-url` and `resource-pack-sha1` in `plugins/AfriTiersPurpur/config.yml` using the SHA-1 supplied with the pack, then restart or reload and reconnect. Players may decline the pack; text labels work without it. Until the pack successfully loads, custom trophy glyphs are omitted. The pack includes the supplied DiaVanilla artwork.

```yaml
resource-pack-url: 'https://YOUR-PUBLIC-HOST/AfriTiers-server-icons-mc1.21.11.zip'
resource-pack-sha1: 'THE-SHA1-SUPPLIED-WITH-THE-ICON-PACK'
```

## Queue integration

AfriTiersLink identifies the actual Minecraft player, checks the existing Discord account link and applies the normal authentication, restrictions, cooldown and queue checks. No new Discord login is required. Personal ticket status uses the existing Link backend configuration and HTTPS certificate; no signing credentials are supplied in this distribution. Offline-mode servers require the authentication policy already configured for AfriTiersLink.

## Build and validation

With JDK 21 and Maven, run `mvn package`. Output: `target/AfriTiersPurpur-1.0.0.jar`. Purpur and PacketEvents are provided dependencies; Gson is shaded. Unit and mocked integration tests cover open-queue completion, forwarding the actual player, unavailable backend behavior, offline authentication, settings persistence, ranking artwork, ticket signatures and shared kit/queue/HTTP rules.

Built against the Purpur 1.21.11 API. A live Purpur startup and a real linked-player queue join still need verification on the target server; no production Minecraft server was accessible for deployment here.

MPL-2.0; see LICENSE and NOTICE. PacketEvents is installed separately under its own license.
