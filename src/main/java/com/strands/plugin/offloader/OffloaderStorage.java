package com.strands.plugin.offloader;

import java.util.Optional;

public interface OffloaderStorage {

    void store(String key, String content);

    Optional<String> retrieve(String key);
}
