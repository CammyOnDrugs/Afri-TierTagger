# Afri-Tiers — complete weekend changelog

13–15 November 2026. Covers the November 13 Codex work and the Afri-Tiers work recovered from Claude, including the November 15 changes. Later changes below supersede the earlier designs where noted. Build and upload status is recorded separately in RELEASE-STATUS.md.

## Mod — Afri-TiersTagger

### Icons, gamemodes and glyphs

- Added four selectable icon sets: Afri-Tiers, PvPTiers, PvPHQ and Minecraft. Each supports all 18 gamemodes.
- Main-tier order: Sword, Axe, Vanilla, UHC, DiaPot, NethPot, SMP, Mace and SpearMace. Subtier order: DiaSMP, DiaVanilla, Cart, Speed, Creeper, Bed, OGV, RocketMace and HoleFight.
- Assigned 72 mode icons and three trophies to Unicode private-use characters U+10FFB3–U+10FFFD. Included a complete adjacent glyph string and a mapping by style and mode.
- Changed the icon representation to support supplementary Unicode characters, which need two Java UTF-16 code units.
- Fixed transparent padding that made icons appear too small. On November 15, rebuilt all 72 icons at 256px from the available source artwork, trimmed and centred them, and gave the website the same files.
- Added RocketMace kit detection using two maces, netherite armour and firework rockets. Added HoleFight detection using two diamond swords, diamond armour, wool, Turtle Master potion and a netherite pickaxe. Conflicting specific kit matches no longer force a guess.

### Queues and account linking

- Added the no-argument `/join-queue` menu and Alerts → Open Queues. The argument form remains available.
- Added per-mode Open/Closed states, join buttons, linked-account status and clickable Discord instructions.
- Added linked status to the AfriTiersLink client notices. Server account, restriction and cooldown checks remain responsible for queue admission.
- Built AfriTiersLink 1.1.3 and its source archive. Prepared Surge's private configuration and backend certificate using the same UUID scheme as LA-Network and Afri-Network.

### Menus and appearance

- Added Liquid Glass, menu colour and animation controls; wider rankings; glass queue and ranking panels; and an RGB/hex tier-colour editor without a Hue slider.
- Added a legacy Settings tab and “Go to Modern Menu” in the legacy interface.
- Added peak-tier, main-tier, subtier and own-tier-in-F5 switches to both menus.
- Added Afri-Tiers branding, the author credit, and circular GitHub, Modrinth and website links. Kept Settings above Done.
- On November 15, moved the newer General settings toward the bottom: peak/main/sub tiers, icon style and own-tier-in-F5. Search Players is last.
- Own-tier-in-F5 is now enabled by default, including migration of existing configurations.
- Added a separate Name Tags switch alongside the Tab List switch.
- Removed tier injection into holograms and other text displays so Fancy Holograms leaderboards are left alone.
- Turning off Rank Numbers now also hides the first-, second- and third-place trophy icons.
- Selecting Minecraft icons forces Liquid Glass off and prevents enabling it while that style is selected.
- Removed Liquid Glass from Search Players and the profile screen it opens. This supersedes the November 13 glass search/profile design.

### Updater and release packaging

- Added Modrinth and GitHub update checks, compatible-game-version selection, checksum checks and downgrade rejection.
- Added an installer helper that waits for Minecraft to exit, checks the downloaded JAR again, backs up the old version, installs the pending version and restores the backup if replacement fails.
- November 13's 2.8.5 artifacts used renamed internal classes and stripped debug metadata, while retaining Fabric entrypoints, mixins, the installer helper and required licences.
- November 15's 2.8.6 artifacts are **not obfuscated**, following the user's later instruction. The earlier request to obfuscate does not apply to this release.
- Kept source and licence notices with the distributable. The requested copyright class and resource remain present.
- Recovered the missing Java 25 build toolchain from IntelliJ's bundled JDK and completed the remaining Minecraft 26.x release builds during this handover.

## Website

### Gamemodes, kits and rankings

- Added RocketMace and HoleFight subtier pages, mode icons, inventory images and full kit descriptions. Corrected both shield descriptions to Mending I.
- Kept all 18 modes in generated rankings data and used the same main/subtier order as the mod.
- Added the four icon styles. November 15 replaced their artwork with the same trimmed 256px images used by the mod.
- Added sticky ranking column headings and retained the production sorting order; fixed the earlier preview losing that order.
- Earlier wider/tinted/outlined preview styling was revised on November 15 to restore the production layout as the base.

### Appearance and Minecraft style

- Added Browser default, Light and Dark themes, a custom accent/menu colour, Liquid Glass and animation preferences saved in the visitor's browser.
- Added a download chooser for the official Modrinth and GitHub pages.
- Added page/menu transitions and player rows entering from right to left, with reduced-motion support.
- Initially placed Appearance beside Account. The latest design uses a compact settings icon beside Search.
- Initially applied glass to most site surfaces. The latest design limits it to the navigation bar, Discord menu and dialogs, using translucent fills, blur, shadow and a top highlight. Reduced-transparency preferences use solid panels.
- Minecraft style has item icons, a hotbar mode selector, a selected-slot marker, hover names, the spyglass search icon, bevelled controls, Minecraft font and a pixel-style scrollbar.
- Minecraft style forces Dark and disables Liquid Glass; its glass switch is locked.
- The gold/iron/copper block podium is now exclusive to Minecraft style. It includes placement, player head, name and points. Other styles use the existing production podium.
- The supplied PvPHQ offline ZIP contained a browser-check page. Claude built the podium from the supplied reference and available stylesheet/textures rather than claiming that ZIP contained the actual leaderboard markup. Afri-Tiers has no win/loss data, so no such line was added.

### Servers and community

- Removed the separate Discord Communities section and made the three Verified Servers cards LA-Network, Afri-Network and SurgeMC.
- Added banners, equal-size icons and Join Discord buttons with Discord icons. Kept the existing Discord dropdown links and IP-copy actions.
- Kept the existing orange verified text rather than adding another blue verification check.

### Policies and footer

- Added View Our Policies, a kit-style policy dialog and footer links for Terms, Privacy, Copyright and Rules.
- Expanded Terms to cover eligibility, linking, testing, corrections, restrictions, appeals, acceptable use and service availability.
- Expanded Privacy to describe collected information, public rankings, purposes, recipients, retention, rights, children and security. The text references POPIA and GDPR; it has not received legal review.
- Expanded Tier List Rules using the supplied allowed/disallowed mod lists, restriction severity ladders, screensharing requirements, in-game rules, platform-wide rules, community chat rules and Ping Equalizer conditions.
- Added lists and expandable rule sections, including mod examples, offence notes, required services and the supplied service re-enable commands.
- Corrected the copied “PVPHQ” community reference to “Afri-Tiers”.
- Ran the supplied writing checks on the policy/handbook prose. A clean phrase scan is not a legal review.

## Discord bot

### November 13 — R44

- Added RocketMace and HoleFight as subtiers with their supplied tester/tier roles, waitlist channels and ticket categories.
- Limited placements and tier suggestions in those modes to HT3 through LT5.
- Added a gray 📤 Leave button only while a queue is open. Kept public `/leave` without staff permission and rejected stale Leave buttons after queue closure.
- Created closed waitlist panels with Current Cooldown automatically, including the two new modes.
- Added independent Surge tester LuckPerms jobs alongside LA-Network and Afri-Network, using the confirmed shared UUIDs.
- Tightened ticket notifications: the Discord ticket must exist in the configured guild/category and the active test record must still match.

### November 15 — R45

- Added staff `/changeign old_name new_name` to move rankings and peak records to a new Minecraft name. Duplicate mode placements merge using the most recently updated placement; timestamp ties keep the higher tier, and the better peak is retained.
- Added five-minute refresh passes for linked Premium accounts, resolving the current name by Mojang UUID and moving stale ranking names. Each pass checks a limited batch; cracked/unlinked players still require staff correction.
- Added API folding of duplicate records belonging to the same linked account, covering stale-name duplicates such as M_BH/M_BHVerity67.
- Added `/staff unlink reason [discord] [username]` with its own staff check. It invalidates link sessions/codes, removes the account link and attempts to reset account roles/nickname while preserving tiers and history.
- Added staff `/forcetest discord username gamemode` to create a testing ticket without joining a queue. It checks the mode/member/name/category and refuses an existing live test for that player and mode.
- Added `/evalpass username discord [gamemode]` for authorised testers/staff. It can infer the mode from the testing ticket and creates `<gamemode>-<playername>` in evaluation category `1571271855464120481`.
- Changed `/rank-add` to accept a Discord member and use their verified linked Minecraft name. A typed-name fallback remains for unlinked players.
- Changed the result message inside testing tickets to ping the testee only.
- Added automatic website Staff/Testers cards from Discord roles, including updates/removal when roles change. Manually maintained `/homepage-staff` and `/homepage-tester` cards are preserved.
- Confirmed configured tester roles exist for all 18 modes, including SpearMace. “Sol” was not identified as a configured mode and remains unresolved.

## Private staff documents and recovered context

- Created a private Word staff handbook using the Afri-Tiers icon. It covers the hierarchy, staff conduct/operations, tester responsibilities and owner-only authority.
- Kept the handbook outside public website/download bundles.
- Normalised “Network Overseer” to “Tierlist Overseer” to match the supplied hierarchy and completed the unfinished owner-authority sentence. These editorial changes are recorded for review.
- Included a plain-text policies/rules copy and a plain-text private handbook copy.
- Recovered five relevant Claude conversations locally, preserving 101 visible messages and three embedded images in a private continuity folder. Internal reasoning and injected skill text were excluded. Credentials are not included in the public release package.
- Recorded the earlier standalone Claude appearance prototypes separately as historical work; they are not silently merged into the latest website source.
- The requested Google Doc in the exact LA Network handbook format remains unfinished: the LA template was not supplied. The Word document is available now.
- The requested additional “one-tone” mod menu icons and online icons in the handbook/rules were explicitly left unfinished by Claude; they are not claimed as delivered.

## Validation and release limits

- November 13: 90 unit tests for each of six mod versions; six isolated obfuscated-client runtime checks; nine bot regressions; Link Maven build; browser checks of four styles and desktop/tablet/phone dimensions; installer checks with test files and a dummy parent process.
- November 15: Claude built/tested 1.21.11, ran five rename/merge tests and inspected the desktop website. This handover reruns the five bot tests, completes the 26.x builds and checks the latest website in a browser. Exact results are in RELEASE-STATUS.md and checks/.
- Earlier runtime results for 2.8.5 do not establish runtime validation of changed 2.8.6 code.
- Live Discord execution of R45 commands, authenticated multiplayer queue joins, F5 tags on a live server, an actual future client auto-update and physical iPhone Safari remain unchecked.
- No database snapshot, account links, queue state or active-test state is included in the release upload. Any live upload must back up the affected files and verify the current live source before replacement.

## Other Claude work this weekend — separate from Afri-Tiers

- A Hazel Crystal Optimizer 1.21.11 port exists under `hco/dist/`. It is not part of the TierTagger release and is not being added to Afri-Tiers public downloads; the supplied Afri-Tiers rules list Hazel Crystal Optimizer as disallowed.
- A separate conversation discussed Google Docs integration. It did not create the requested Afri-Tiers Google Doc.
