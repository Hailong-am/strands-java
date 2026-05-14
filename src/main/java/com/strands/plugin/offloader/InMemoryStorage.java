package com.strands.plugin.offloader;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStorage implements OffloaderStorage {

    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    public void store(String key, String content) {
        store.put(key, content);
    }

    @Override
    public Optional<String> retrieve(String key) {
        return Optional.ofNullable(store.get(key));
    }
}
