package com.skalpha.tiertagger.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class TierList {
   private static final Map<String, TierList> BY_ID = new LinkedHashMap<>();
   private static final Map<String, TierList> BY_NORMALIZED_URL = new ConcurrentHashMap<>();
   private static final List<TierList> ALL = new ArrayList<>();
   private static final AtomicBoolean LOADING = new AtomicBoolean(false);
   private static volatile boolean loadedSuccessfully = false;
   private static CompletableFuture<Void> loadFuture;
   private final String id;
   private final String name;
   private final String url;
   private final String iconUrl;
   private final char icon;

   private TierList(String id, String name, String url, String iconUrl, char icon) {
      this.id = id;
      this.name = name;
      this.url = url;
      this.iconUrl = iconUrl;
      this.icon = icon;
   }

   public static List<TierList> values() {
      ensureLoadedAsync();
      return Collections.unmodifiableList(ALL);
   }

   private static void ensureLoadedAsync() {
      if (!loadedSuccessfully && !LOADING.get()) {
         if (LOADING.compareAndSet(false, true)) {
            loadFuture = CompletableFuture.runAsync(TierList::loadFromApi);
         }
      }
   }

   private static void loadFromApi() {
      synchronized (TierList.class) {
         BY_ID.clear();
         BY_NORMALIZED_URL.clear();
         ALL.clear();
         loadedSuccessfully = false;
      }

      LOADING.set(false);
   }

   private static String normalize(String url) {
      return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
   }

   public String getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public String getUrl() {
      return this.url;
   }

   public String getIconUrl() {
      return this.iconUrl;
   }

   public char getIcon() {
      return this.icon;
   }
}
