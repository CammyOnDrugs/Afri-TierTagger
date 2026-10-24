package com.skalpha.tiertagger.queue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

                                                                                     
public final class QueueTracker {
    private boolean initialized;
    private Instant latest = Instant.MIN;
    private final Set<String> seen = new LinkedHashSet<>();

    public List<QueueFeed.Entry> accept(QueueFeed feed, Instant now) {
        if (feed.generatedAt().isBefore(latest) || feed.generatedAt().isAfter(now.plusSeconds(60))
                || Duration.between(feed.generatedAt(), now).compareTo(Duration.ofMinutes(2)) > 0)
            return List.of();
        List<QueueFeed.Entry> opened = new ArrayList<>();
        for (QueueFeed.Entry entry : feed.queues()) {
            if (entry.eventId() == null) continue;
            String key = entry.mode() + ":" + entry.eventId();
            boolean fresh = seen.add(key);
            if (initialized && fresh && entry.open() && entry.openedAt() != null
                    && Duration.between(entry.openedAt(), now).compareTo(Duration.ofMinutes(2)) <= 0)
                opened.add(entry);
        }
        while (seen.size() > 256) seen.remove(seen.iterator().next());
        initialized = true;
        latest = feed.generatedAt();
        return List.copyOf(opened);
    }
}
