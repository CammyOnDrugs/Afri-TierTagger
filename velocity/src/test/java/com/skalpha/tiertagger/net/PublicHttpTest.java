package com.skalpha.tiertagger.net;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PublicHttpTest {
    static class Subscription implements Flow.Subscription {
        volatile boolean cancelled;
        public void request(long count) {}
        public void cancel() { cancelled = true; }
    }
    @Test void decodesUtf8AcrossChunks() throws Exception {
        var body = new PublicHttp.LimitedBody(64, Duration.ofSeconds(2));
        body.onSubscribe(new Subscription());
        byte[] text = "ǫᴜᴇᴜᴇ".getBytes(StandardCharsets.UTF_8);
        body.onNext(List.of(ByteBuffer.wrap(text, 0, 1)));
        body.onNext(List.of(ByteBuffer.wrap(text, 1, text.length - 1)));
        body.onComplete();
        assertEquals("ǫᴜᴇᴜᴇ",body.getBody().toCompletableFuture().get());
    }
    @Test void overflowCancelsUpstreamBeforeAccumulatingMore() {
        var body = new PublicHttp.LimitedBody(3, Duration.ofSeconds(2));
        var subscription = new Subscription();
        body.onSubscribe(subscription);
        body.onNext(List.of(ByteBuffer.wrap(new byte[4])));
        assertTrue(subscription.cancelled);
        assertTrue(body.getBody().toCompletableFuture().isCompletedExceptionally());
    }
    @Test void stalledBodyCancelsUpstream() {
        var body = new PublicHttp.LimitedBody(64, Duration.ofMillis(30));
        var subscription = new Subscription();
        body.onSubscribe(subscription);
        assertThrows(ExecutionException.class, () -> body.getBody().toCompletableFuture().get(2, TimeUnit.SECONDS));
        assertTrue(subscription.cancelled);
    }
}
