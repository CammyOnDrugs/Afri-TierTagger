package com.skalpha.tiertagger.net;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

                                                                                 
public final class PublicHttp {
    private PublicHttp() {}

    public static HttpResponse.BodyHandler<String> limitedUtf8(int maxBytes) {
        return info -> new LimitedBody(maxBytes, Duration.ofSeconds(12));
    }

    public static HttpResponse.BodyHandler<byte[]> limitedBytes(int maxBytes) {
        return info -> new LimitedData<>(maxBytes,Duration.ofSeconds(12),value->value);
    }

    static final class LimitedBody extends LimitedData<String> {
        LimitedBody(int maxBytes,Duration deadline){super(maxBytes,deadline,value->new String(value,StandardCharsets.UTF_8));}
    }

    static class LimitedData<T> implements HttpResponse.BodySubscriber<T> {
        private final int maxBytes;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final CompletableFuture<T> body = new CompletableFuture<>();
        private final java.util.function.Function<byte[],T> convert;
        private Flow.Subscription subscription;

        LimitedData(int maxBytes, Duration deadline,java.util.function.Function<byte[],T> convert) {
            this.maxBytes = maxBytes;
            this.convert=convert;
            CompletableFuture.delayedExecutor(deadline.toMillis(), TimeUnit.MILLISECONDS)
                .execute(() -> fail(new java.net.http.HttpTimeoutException("Public response body timed out")));
        }

        public CompletionStage<T> getBody() { return body; }

        public synchronized void onSubscribe(Flow.Subscription incoming) {
            if (subscription != null || body.isDone()) { incoming.cancel(); return; }
            subscription = incoming;
            incoming.request(1);
        }

        public synchronized void onNext(List<ByteBuffer> buffers) {
            if (body.isDone()) return;
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > maxBytes - bytes.size()) {
                    fail(new IllegalArgumentException("Public response body too large"));
                    return;
                }
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }

        public void onError(Throwable error) { fail(error); }

        public synchronized void onComplete() {
            body.complete(convert.apply(bytes.toByteArray()));
        }

        private synchronized void fail(Throwable error) {
            if (body.isDone()) return;
            if (subscription != null) subscription.cancel();
            body.completeExceptionally(error);
        }
    }
}
