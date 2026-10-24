package com.skalpha.tiertagger.queue;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record QueuePayload(String json) implements CustomPayload {
    public static final Id<QueuePayload> ID = new Id<>(Identifier.of("afritiers", "queue"));
    public static final PacketCodec<RegistryByteBuf, QueuePayload> CODEC = PacketCodec.tuple(PacketCodecs.string(16_000), QueuePayload::json, QueuePayload::new);
    @Override public Id<? extends CustomPayload> getId() { return ID; }

    public record Hello(String version) implements CustomPayload {
        public static final Id<Hello> ID = new Id<>(Identifier.of("afritiers", "hello"));
        public static final PacketCodec<RegistryByteBuf, Hello> CODEC = PacketCodec.tuple(PacketCodecs.string(24), Hello::version, Hello::new);
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
