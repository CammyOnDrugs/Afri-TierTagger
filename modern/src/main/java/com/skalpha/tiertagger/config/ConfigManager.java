package com.skalpha.tiertagger.config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;
public final class ConfigManager<T> {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private final T config;
    private ConfigManager(Class<T> type, String name) {
        path = FabricLoader.getInstance().getConfigDir().resolve(name + ".json");
        T loaded;
        try {
            loaded = Files.exists(path) ? JSON.fromJson(Files.readString(path), type) : type.getConstructor().newInstance();
            if (loaded == null) loaded = type.getConstructor().newInstance();
        } catch (Exception error) {
            try {
                if (Files.exists(path)) Files.copy(path, path.resolveSibling(name + ".invalid-" + System.currentTimeMillis() + ".json"));
                loaded = type.getConstructor().newInstance();
            } catch (Exception failure) { throw new IllegalStateException("Cannot load Afri-Tiers settings", failure); }
        }
        config = loaded;
    }
    public static <T> ConfigManager<T> createDefault(Class<T> type, String name) { return new ConfigManager<>(type, name); }
    public T getConfig() { return config; }
    public synchronized void saveConfig() {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, JSON.toJson(config));
            try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException error) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception error) { throw new IllegalStateException("Cannot save Afri-Tiers settings", error); }
    }
}
