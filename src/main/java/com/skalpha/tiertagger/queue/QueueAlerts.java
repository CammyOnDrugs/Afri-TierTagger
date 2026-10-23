package com.skalpha.tiertagger.queue;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.skalpha.tiertagger.TierTagger;
import com.skalpha.tiertagger.kit.KitIdentifier;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class QueueAlerts {
    private static final URI ENDPOINT = URI.create("https://afritiers.co.za/api/queues");
    private static final Map<String, QueueFeed.Entry> queues = new HashMap<>();
    private static final ArrayDeque<QueueFeed.Entry> pending = new ArrayDeque<>();
    private static QueueTracker tracker = new QueueTracker();
    private static Object world;
    private static int session;
    private static boolean inFlight;
    private static long nextPoll;
    private static int soundTicks;

    private QueueAlerts() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(QueueAlerts::tick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
            ClientCommandManager.literal("join-queue")
                .then(ClientCommandManager.argument("gamemode", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        queues.values().stream().filter(QueueFeed.Entry::open)
                            .map(QueueFeed.Entry::mode).sorted().forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        String requested = StringArgumentType.getString(context, "gamemode").toLowerCase(Locale.ROOT);
                        String mode = switch (requested) {
                            case "pot", "dia_pot" -> "diapot";
                            case "op", "nethop" -> "nethpot";
                            case "minecart" -> "cart";
                            case "dia_smp" -> "diasmp";
                            case "spearmace" -> "spear";
                            default -> requested;
                        };
                        QueueFeed.Entry entry = queues.get(mode);
                        if (entry == null || !entry.open()) {
                            context.getSource().sendError(Text.literal("That queue is closed or its status is unavailable."));
                            return 0;
                        }
                        // Joining remains authenticated by Discord's existing queue button.
                        context.getSource().sendFeedback(Text.literal("Use Join Queue in the Discord waitlist channel."));
                        MinecraftClient client = context.getSource().getClient();
                        ConfirmLinkScreen.open(client.currentScreen, entry.joinUrl().toString(), true);
                        return 1;
                    }))));
    }

    private static void tick(MinecraftClient client) {
        if (client.world != world) {
            world = client.world;
            session++;
            inFlight = false;
            nextPoll = 0;
            tracker = new QueueTracker();
            queues.clear();
            pending.clear();
            soundTicks = 0;
            KitIdentifier.clear();
        }
        if (client.world == null || client.player == null) return;
        if (soundTicks == 0 && !pending.isEmpty()) {
            QueueFeed.Entry entry = pending.removeFirst();
            String mode = smallCaps(entry.mode().equals("spear") ? "spearmace" : entry.mode());
            client.inGameHud.setTitleTicks(0, 60, 0);
            client.inGameHud.setSubtitle(Text.literal("ᴅᴏ /ᴊᴏɪɴ-ǫᴜᴇᴜᴇ " + mode + " ᴛᴏ ᴊᴏɪɴ ᴛʜᴇ ᴡᴀɪᴛʟɪѕᴛ!").formatted(Formatting.WHITE));
            client.inGameHud.setTitle(Text.literal(mode + " ǫᴜᴇᴜᴇ ʜᴀѕ ᴊᴜѕᴛ ᴏᴘᴇɴᴇᴅ!").formatted(Formatting.RED));
            soundTicks = 60;
        }
        if (soundTicks > 0) {
            if (soundTicks % 2 == 0)
                client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F));
            soundTicks--;
        }
        long now = System.nanoTime();
        if (!inFlight && now >= nextPoll) {
            inFlight = true;
            nextPoll = now + Duration.ofSeconds(5).toNanos();
            int requestSession = session;
            HttpRequest request = HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json").header("User-Agent", "Afri-TiersTagger/2.5.0").GET().build();
            TierTagger.getClient().sendAsync(request, com.skalpha.tiertagger.net.PublicHttp.limitedUtf8(65_536))
                .thenApply(response -> {
                    if (response.statusCode() != 200) throw new IllegalArgumentException("Queue status unavailable");
                    return QueueFeed.parse(response.body());
                }).whenComplete((feed, error) -> client.execute(() -> {
                    if (requestSession != session) return;
                    inFlight = false;
                    if (error != null) { queues.clear(); return; }
                    queues.clear();
                    feed.queues().forEach(entry -> queues.put(entry.mode(), entry));
                    for (QueueFeed.Entry entry : tracker.accept(feed, Instant.now())) {
                        if (pending.size() < QueueFeed.MODES.size()) pending.addLast(entry);
                    }
                }));
        }
    }

    static String smallCaps(String value) {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        String[] glyphs = {"ᴀ","ʙ","ᴄ","ᴅ","ᴇ","ꜰ","ɢ","ʜ","ɪ","ᴊ","ᴋ","ʟ","ᴍ","ɴ","ᴏ","ᴘ","ǫ","ʀ","ѕ","ᴛ","ᴜ","ᴠ","ᴡ","x","ʏ","ᴢ"};
        StringBuilder text = new StringBuilder();
        for (char character : value.toLowerCase(Locale.ROOT).toCharArray()) {
            int index = alphabet.indexOf(character);
            text.append(index < 0 ? String.valueOf(character) : glyphs[index]);
        }
        return text.toString();
    }
}
