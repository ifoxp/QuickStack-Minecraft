package top.ifoxp.quickstack.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;
import top.ifoxp.quickstack.QuickStack;
import top.ifoxp.quickstack.platform.Services;

/** Plain JSON config files in the loader's config folder; no config library needed on either loader. */
final class ConfigIO {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigIO() {
    }

    static Path path(String fileName) {
        return Services.PLATFORM.getConfigDir().resolve(fileName);
    }

    static <T> T read(String fileName, Class<T> type, Supplier<T> defaults) {
        Path file = path(fileName);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                T value = GSON.fromJson(reader, type);
                if (value != null) {
                    return value;
                }
            } catch (Exception e) {
                QuickStack.LOG.warn("Could not read {}, using defaults", file, e);
            }
        }
        return defaults.get();
    }

    /** Writes to a temp file first and moves it over, so a crash mid-write never leaves a truncated config. */
    static void write(String fileName, Object value) {
        Path file = path(fileName);
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(fileName + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(value, writer);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            QuickStack.LOG.warn("Could not write {}", file, e);
        }
    }
}
