package com.strands.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Watches a directory for file changes and triggers tool reload callbacks.
 * Useful for development-time hot-reloading of tool definitions.
 */
public class ToolWatcher implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ToolWatcher.class);

    private final Path directory;
    private final Consumer<Path> onChanged;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread watchThread;

    public ToolWatcher(Path directory, Consumer<Path> onChanged) {
        this.directory = directory;
        this.onChanged = onChanged;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            watchThread = new Thread(this::watchLoop, "tool-watcher");
            watchThread.setDaemon(true);
            watchThread.start();
            log.info("ToolWatcher started for: {}", directory);
        }
    }

    public void stop() {
        running.set(false);
        if (watchThread != null) {
            watchThread.interrupt();
        }
    }

    @Override
    public void close() {
        stop();
    }

    public boolean isRunning() {
        return running.get();
    }

    private void watchLoop() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            while (running.get()) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path changedFile = directory.resolve(pathEvent.context());

                    log.debug("Tool file changed: {} ({})", changedFile, event.kind());
                    try {
                        onChanged.accept(changedFile);
                    } catch (Exception e) {
                        log.error("Error processing tool change for: {}", changedFile, e);
                    }
                }

                if (!key.reset()) {
                    log.warn("Watch key no longer valid for: {}", directory);
                    break;
                }
            }
        } catch (IOException e) {
            log.error("ToolWatcher failed for: {}", directory, e);
        }
    }
}
