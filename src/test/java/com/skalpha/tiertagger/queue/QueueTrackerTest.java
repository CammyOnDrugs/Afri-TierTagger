package com.skalpha.tiertagger.queue;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QueueTrackerTest {
    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");
    private static final String A = "a".repeat(32), B = "b".repeat(32);
    private static QueueFeed feed(boolean open, String event, Instant generated) {
        return new QueueFeed(generated, List.of(new QueueFeed.Entry("sword", open, event,
            event == null ? null : NOW, URI.create("https://discord.com/channels/123456789012345678/123456789012345679"))));
    }

    @Test void firstSnapshotAndRepeatedEventsAreSilent() {
        QueueTracker tracker = new QueueTracker();
        assertTrue(tracker.accept(feed(true, A, NOW), NOW).isEmpty());
        assertTrue(tracker.accept(feed(true, A, NOW.plusSeconds(5)), NOW.plusSeconds(5)).isEmpty());
    }
    @Test void closeReopenAlertsOnceWithNewIdentity() {
        QueueTracker tracker = new QueueTracker();
        tracker.accept(feed(false, null, NOW), NOW);
        assertEquals(1, tracker.accept(feed(true, A, NOW.plusSeconds(1)), NOW.plusSeconds(1)).size());
        assertTrue(tracker.accept(feed(true, A, NOW.plusSeconds(2)), NOW.plusSeconds(2)).isEmpty());
        tracker.accept(feed(false, null, NOW.plusSeconds(3)), NOW.plusSeconds(3));
        assertEquals(1, tracker.accept(feed(true, B, NOW.plusSeconds(4)), NOW.plusSeconds(4)).size());
        assertTrue(tracker.accept(feed(true, A, NOW.plusSeconds(5)), NOW.plusSeconds(5)).isEmpty());
    }
    @Test void staleOutOfOrderAndFutureResponsesCannotTriggerAlerts() {
        QueueTracker tracker = new QueueTracker();
        tracker.accept(feed(false, null, NOW), NOW);
        assertTrue(tracker.accept(feed(true, A, NOW.minusSeconds(1)), NOW).isEmpty());
        assertTrue(tracker.accept(feed(true, A, NOW.plusSeconds(121)), NOW).isEmpty());
        assertTrue(tracker.accept(feed(true, A, NOW), NOW.plusSeconds(121)).isEmpty());
    }
    @Test void legacyOpenWithoutAnEventIsSilent() {
        QueueTracker tracker = new QueueTracker();
        tracker.accept(feed(false, null, NOW), NOW);
        assertTrue(tracker.accept(feed(true, null, NOW.plusSeconds(1)), NOW.plusSeconds(1)).isEmpty());
    }
    private String json(String link, String event, String mode) {
        return """
            {"ok":true,"version":1,"generatedAt":"2026-10-23T10:00:00Z","queues":[
            {"mode":"%s","open":true,"eventId":"%s","openedAt":"2026-10-23T10:00:00Z","joinUrl":"%s"}]}
            """.formatted(mode,event,link);
    }
    @Test void untrustedLinksUnknownModesAndMalformedEventsAreRejected() {
        String link = "https://discord.com/channels/123456789012345678/123456789012345679";
        assertEquals("sword",QueueFeed.parse(json(link,A,"sword")).queues().getFirst().mode());
        for (String bad : List.of("https://evil.example/", "https://discord.com@evil.example/channels/1/2",link+"?token=x", "http://discord.com/channels/1/2"))
            assertThrows(IllegalArgumentException.class, () -> QueueFeed.parse(json(bad,A,"sword")));
        assertThrows(IllegalArgumentException.class, () -> QueueFeed.parse(json(link,"invalid","sword")));
        assertThrows(IllegalArgumentException.class, () -> QueueFeed.parse(json(link,A,"unknown")));
    }
}
