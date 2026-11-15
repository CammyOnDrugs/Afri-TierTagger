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
import java.util.Set;
import java.util.HashSet;
import com.google.gson.JsonParser;
import com.skalpha.tiertagger.config.TierTaggerConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class QueueAlerts {
    private static final URI ENDPOINT = URI.create("https://afritiers.co.za/api/queues");
    private static final Map<String, QueueFeed.Entry> queues = new HashMap<>();
    private static final ArrayDeque<QueueFeed.Entry> pending = new ArrayDeque<>();
    private static IIIIII tracker = new IIIIII();
    private static Object world;
    private static Object connection;
    private static boolean helloSent;
    private static Boolean linked;
    private static long feedReceived, linkReceived;
    private static final Set<String> seenTickets = new HashSet<>();
    private static final ArrayDeque<Ticket> tickets = new ArrayDeque<>();
    private record Ticket(String event, String username, String mode) {}
    private static int session;
    private static boolean inFlight;
    private static long nextPoll;
    private static int soundTicks;
    private static int displayTicks;

    private QueueAlerts() {}

    public static void init() {
        PayloadTypeRegistry.playS2C().register(QueuePayload.ID, QueuePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(QueuePayload.Hello.ID, QueuePayload.Hello.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(QueuePayload.ID, (payload, context) -> acceptTickets(payload.json()));
        ClientTickEvents.END_CLIENT_TICK.register(QueueAlerts::tick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
            ClientCommandManager.literal("join-queue").executes(context -> { var client=context.getSource().getClient(); client.setScreen(new QueueScreen(client.screen)); return 1; })
                .then(ClientCommandManager.argument("gamemode", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        QueueFeed.openModes(queues.values(), builder.getRemaining()).forEach(builder::suggest);
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
                        if (!QueueFeed.MODES.contains(mode)) {
                            context.getSource().sendError(Component.literal("Unknown gamemode."));
                            return 0;
                        }
                        Minecraft client = context.getSource().getClient();
                        if (client.getConnection() == null || !ClientPlayNetworking.canSend(QueuePayload.Hello.ID)) {
                            context.getSource().sendError(Component.literal("Use /join-queue on LA-Network, Afri-Network or SurgeMC with the updated AfriTiersLink plugin."));
                            return 0;
                        }
                        client.getConnection().sendCommand("afritierslink:join-queue " + mode);
                        return 1;
                    }))));
    }

    private static void tick(Minecraft client) {
        if (client.level != world) { world = client.level; KitIdentifier.clear(); helloSent = false; }
        if (client.getConnection() != connection) {
            connection = client.getConnection();
            session++;
            inFlight = false;
            nextPoll = 0;
            tracker = new IIIIII();
            queues.clear();
            pending.clear();
            soundTicks = 0;
            displayTicks = 0;
            seenTickets.clear(); tickets.clear(); helloSent = false; linked=null;feedReceived=0;linkReceived=0;
        }
        if (client.level == null || client.player == null) return;
        if (!helloSent && ClientPlayNetworking.canSend(QueuePayload.Hello.ID)) {
            ClientPlayNetworking.send(new QueuePayload.Hello("2.7.1")); helloSent = true;
        }
        TierTaggerConfig config = TierTagger.getManager().getConfig();
        if (!config.isWaitlistAlerts()) { pending.clear(); soundTicks = 0; }
        if (!config.isWaitlistSound()) soundTicks = 0;
        if (!config.isTicketAlerts()) tickets.clear();
        if (displayTicks > 0) displayTicks--;
        if (displayTicks == 0 && !tickets.isEmpty()) {
            Ticket ticket = tickets.removeFirst();
            String mode = ticket.mode().equals("spear") ? "Spearmace" : ticket.mode().toUpperCase(Locale.ROOT);
            if (config.getTicketLocation() == TierTaggerConfig.AlertLocation.CHAT)
                client.gui.getChat().addMessage(Component.literal(ticket.username() + " Your Testing Ticket for " + mode + " is open").withStyle(ChatFormatting.GREEN));
            else {
                client.gui.setTimes(0, 60, 0);
                client.gui.setSubtitle(Component.literal("Your Testing Ticket for " + mode + " is open").withStyle(ChatFormatting.WHITE));
                client.gui.setTitle(Component.literal(ticket.username()).withStyle(ChatFormatting.GREEN));
                displayTicks = 60;
            }
            soundTicks = 0;
            if (config.isTicketSound()) client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F));
        } else if (displayTicks == 0 && !pending.isEmpty()) {
            QueueFeed.Entry entry = pending.removeFirst();
            String mode = smallCaps(entry.mode().equals("spear") ? "spearmace" : entry.mode());
            Component subtitle = Component.literal("ᴅᴏ /ᴊᴏɪɴ-ǫᴜᴇᴜᴇ " + mode + " ᴛᴏ ᴊᴏɪɴ ᴛʜᴇ ᴡᴀɪᴛʟɪѕᴛ!").withStyle(ChatFormatting.WHITE);
            Component title = Component.literal(mode + " ǫᴜᴇᴜᴇ ʜᴀѕ ᴊᴜѕᴛ ᴏᴘᴇɴᴇᴅ!").withStyle(ChatFormatting.RED);
            if (config.getWaitlistLocation() == TierTaggerConfig.AlertLocation.CHAT)
                client.gui.getChat().addMessage(title.copy().append(" ").append(subtitle));
            else {
                client.gui.setTimes(0, 60, 0);
                client.gui.setSubtitle(subtitle); client.gui.setTitle(title);
            }
            displayTicks = 60;
            soundTicks = config.isWaitlistSound() ? 60 : 0;
        }
        if (soundTicks > 0) {
            if (soundTicks % 2 == 0)
                client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F));
            soundTicks--;
        }
        long now = System.nanoTime();
        if (!inFlight && now >= nextPoll) {
            inFlight = true;
            nextPoll = now + Duration.ofSeconds(5).toNanos();
            int requestSession = session;
            HttpRequest request = HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json").header("User-Agent", "Afri-TiersTagger/2.7.1").GET().build();
            TierTagger.getClient().sendAsync(request, com.skalpha.tiertagger.net.网络.limitedUtf8(65_536))
                .thenApply(response -> {
                    if (response.statusCode() != 200) throw new IllegalArgumentException("Queue status unavailable");
                    return QueueFeed.parse(response.body());
                }).whenComplete((feed, error) -> client.execute(() -> {
                    if (requestSession != session) return;
                    inFlight = false;
                    if (error != null) { queues.clear(); return; }
                    queues.clear();feedReceived=System.nanoTime();
                    feed.queues().forEach(entry -> queues.put(entry.mode(), entry));
                    for (QueueFeed.Entry entry : tracker.accept(feed, Instant.now())) {
                        if (TierTagger.getManager().getConfig().isWaitlistAlerts() && pending.size() < QueueFeed.MODES.size()) pending.addLast(entry);
                    }
                }));
        }
    }

    private static void acceptTickets(String json) {
        try {
            var root = JsonParser.parseString(json).getAsJsonObject();
            if (!"tickets".equals(root.get("type").getAsString())) return;
            if(root.has("linked")&&root.get("linked").isJsonPrimitive()&&root.getAsJsonPrimitive("linked").isBoolean()){linked=root.get("linked").getAsBoolean();linkReceived=System.nanoTime();}
            var rows = root.getAsJsonArray("tickets");
            if (rows.size() > QueueFeed.MODES.size()) return;
            var parsed = new java.util.ArrayList<Ticket>();
            for (var element : rows) {
                var row = element.getAsJsonObject();
                String event = row.get("eventId").getAsString();
                String mode = row.get("mode").getAsString();
                String username = row.get("username").getAsString();
                if (!event.matches("[a-f0-9]{64}") || !QueueFeed.MODES.contains(mode) || !username.matches("[A-Za-z0-9_]{1,16}")) return;
                parsed.add(new Ticket(event, username, mode));
            }
            for (Ticket ticket : parsed) {
                var player=Minecraft.getInstance().player;
                if(player==null||!player.getGameProfile().name().equalsIgnoreCase(ticket.username()))continue;
                if (seenTickets.add(ticket.event()) && TierTagger.getManager().getConfig().isTicketAlerts() && tickets.size() < 16) tickets.addLast(ticket);
            }
        } catch (RuntimeException ignored) {}
    }

    public static Boolean linked(){return System.nanoTime()-linkReceived<Duration.ofSeconds(45).toNanos()?linked:null;}
    public static boolean feedOnline(){return feedReceived!=0&&System.nanoTime()-feedReceived<Duration.ofSeconds(15).toNanos();}
    public static boolean isOpen(String mode){return feedOnline()&&queues.containsKey(mode)&&queues.get(mode).open();}
    public static boolean supported(){return ClientPlayNetworking.canSend(QueuePayload.Hello.ID);}
    public static void join(String mode){
        if(!isOpen(mode)||!supported()||Boolean.FALSE.equals(linked()))return;
        var client=Minecraft.getInstance();if(client.getConnection()!=null)client.getConnection().sendCommand("afritierslink:join-queue "+mode);
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
