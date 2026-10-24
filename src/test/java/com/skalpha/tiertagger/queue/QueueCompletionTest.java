package com.skalpha.tiertagger.queue;
import java.time.Instant;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class QueueCompletionTest {
 @Test void onlyOpenQueuesMatchingPrefixAreSuggested(){
  var now=Instant.now();var link=URI.create("https://discord.com/channels/123456789012345678/123456789012345679");
  var entries=List.of(new QueueFeed.Entry("sword",true,"a".repeat(32),now,link),new QueueFeed.Entry("smp",false,"b".repeat(32),now,link),new QueueFeed.Entry("diavanilla",true,"c".repeat(32),now,link));
  assertEquals(List.of("diavanilla","sword"),QueueFeed.openModes(entries,""));
  assertEquals(List.of("sword"),QueueFeed.openModes(entries,"S"));
  assertEquals(List.of(),QueueFeed.openModes(entries,"sm"));
 }
}
