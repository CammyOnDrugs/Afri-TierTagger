package com.skalpha.tiertagger.queue;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record QueuePayload(String json) implements CustomPacketPayload {
    public static final Type<QueuePayload> ID = new Type<>(Identifier.fromNamespaceAndPath("afritiers", "queue"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QueuePayload> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(16_000), QueuePayload::json, QueuePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    public record Hello(String version) implements CustomPacketPayload {
        public static final Type<Hello> ID = new Type<>(Identifier.fromNamespaceAndPath("afritiers", "hello"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Hello> CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(24), Hello::version, Hello::new);
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }
}
