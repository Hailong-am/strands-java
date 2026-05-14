package com.strands.plugin.offloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class FileStorage implements OffloaderStorage {

    private final Path directory;

    public FileStorage(Path directory) {
        this.directory = directory;
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create offloader storage directory", e);
        }
    }

    @Override
    public void store(String key, String content) {
        try {
            Files.writeString(directory.resolve(key + ".txt"), content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store offloaded content: " + key, e);
        }
    }

    @Override
    public Optional<String> retrieve(String key) {
        Path file = directory.resolve(key + ".txt");
        if (Files.exists(file)) {
            try {
                return Optional.of(Files.readString(file));
            } catch (IOException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
