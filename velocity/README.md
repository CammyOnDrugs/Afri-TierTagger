# Afri-Tiers Velocity 1.0.0

For **Velocity 3.5.1**, Java 21 and Minecraft **1.21.11** clients. Displays Afri-Tiers tags without requiring the client mod.

Install `AfriTiersVelocity-1.0.0.jar` and [PacketEvents for Velocity 2.14.0](https://github.com/retrooper/packetevents/releases/tag/v2.14.0) in the proxy's `plugins` folder, then restart the proxy.

Run **`/afritiers-settings`** in-game. Its clickable menu saves preferences for your UUID: enabled, nametags, tab list, gamemode, highest-tier fallback, retired tiers, gamemode icons, kit identification, tier colors, waitlist/ticket alerts, Chat/Title placement and sounds.

**Rank numbers**, **rank trophy icons**, and **gold/silver/bronze colors** each have their own switch. With podium colors off, numbers are gray. Positions always use the overall All Modes leaderboard, independent of selected gamemode. Profile-search gamemode rows show their tier without a rank number.

`/afritiers <player>` searches a public profile. `/join-queue <mode>` forwards to the current backend's `afritierslink:join-queue` command. `/afritiersreload` reloads public rankings and proxy configuration; it requires `afritiers.reload`.

## Queue integration

Keep **AfriTiersLink 1.1.0 or newer** installed and configured on each supported Minecraft backend. The existing backend verifies the actual player session and Discord link, signs the bot request, applies restrictions/cooldowns and joins the normal Discord queue. Its private testing-ticket messages are consumed by this proxy plugin for vanilla players. The proxy does not accept a client-supplied Discord identity or require another Discord sign-in.

Client-mod users continue using their client display and alert settings, avoiding duplicate tags and alerts. Their client `/afritiers-settings` command opens the client screen.

## Icons

The supplied `AfriTiers-Velocity-icons-mc1.21.11.zip` contains the original gamemode and trophy artwork. Host the ZIP at a direct HTTPS download URL and set `resourcePackUrl` and its SHA-1 in `plugins/afritiersvelocity/config.json`. Then restart the proxy or reload and reconnect. The pack is optional; until successfully loaded, labels use text and no custom trophy glyphs are sent. Gamemode icons and rank trophies can be disabled separately.

```json
{
  "resourcePackUrl": "https://YOUR-PUBLIC-HOST/AfriTiers-Velocity-icons-mc1.21.11.zip",
  "resourcePackSha1": "THE-SHA1-SUPPLIED-WITH-THE-ICON-PACK"
}
```

Kit identification uses only synchronized inventory and equipment packets. Other players' hidden inventories are unavailable, so uncertain kits fall back to the chosen gamemode. Observations remain in proxy memory and are never uploaded.

## Build

With JDK 21 and Maven installed, run `mvn package`. The plugin JAR is written to `target/`. Velocity and PacketEvents are provided dependencies; Gson is shaded into the plugin. Tests cover ranking order and colors, independent settings, persistence, queue forwarding, private ticket validation/deduplication, packet lengths, team visibility and the shared kit/queue rules.

The plugin was also started and stopped on an isolated Velocity 3.5.1 proxy with PacketEvents 2.14.0. Production installation and an actual authenticated queue join require access to the Minecraft servers.

Public source uses MPL-2.0; see `LICENSE` and `NOTICE`. PacketEvents is distributed separately under GPL-3.0 and Velocity under GPL-3.0 with an MIT API.
