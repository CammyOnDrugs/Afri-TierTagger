package com.skalpha.tiertagger.queue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

                                                                                 
public record QueueFeed(Instant generatedAt, List<Entry> queues) {
    public static final Set<String> MODES = Set.of("sword", "vanilla", "axe", "diapot", "nethpot",
            "uhc", "smp", "mace", "ogv", "speed", "diasmp", "creeper", "cart", "spear", "bed", "diavanilla");

    public record Entry(String mode, boolean open, String eventId, Instant openedAt, URI joinUrl) {}

    public static QueueFeed parse(String json) {
        if (json.length() > 65_536) throw new IllegalArgumentException("Queue feed too large");
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.get("ok").getAsBoolean() || root.get("version").getAsInt() != 1)
            throw new IllegalArgumentException("Unsupported queue feed");
        Instant generated = Instant.parse(root.get("generatedAt").getAsString());
        var array = root.getAsJsonArray("queues");
        if (array.size() > MODES.size()) throw new IllegalArgumentException("Too many queues");
        List<Entry> entries = new ArrayList<>();
        Set<String> modes = new HashSet<>();
        for (var element : array) {
            JsonObject entry = element.getAsJsonObject();
            String mode = entry.get("mode").getAsString();
            if (!MODES.contains(mode) || !modes.add(mode)) throw new IllegalArgumentException("Unknown or duplicate mode");
            URI link = URI.create(entry.get("joinUrl").getAsString());
            if (!link.toString().matches("https://discord\\.com/channels/[0-9]{15,22}/[0-9]{15,22}"))
                throw new IllegalArgumentException("Invalid queue link");
            boolean open = entry.get("open").getAsBoolean();
            String event = entry.get("eventId").isJsonNull() ? null : entry.get("eventId").getAsString();
            Instant opened = entry.get("openedAt").isJsonNull() ? null : Instant.parse(entry.get("openedAt").getAsString());
            if (event != null && (!event.matches("[a-f0-9]{32}") || opened == null || opened.isAfter(generated)))
                throw new IllegalArgumentException("Invalid queue event");
            entries.add(new Entry(mode, open, event, opened, link));
        }
        return new QueueFeed(generated, List.copyOf(entries));
    }
}
