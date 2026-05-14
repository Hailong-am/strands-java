package com.strands.event;

import com.strands.hook.HookProvider;
import com.strands.hook.HookRegistry;
import com.strands.hook.events.AfterInvocationEvent;
import com.strands.hook.events.AfterModelCallEvent;
import com.strands.types.exceptions.ModelThrottledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelRetryStrategy implements HookProvider {

    private static final Logger log = LoggerFactory.getLogger(ModelRetryStrategy.class);

    private final int maxAttempts;
    private final double initialDelay;
    private final double maxDelay;
    private int currentAttempt;

    public ModelRetryStrategy() {
        this(6, 4.0, 240.0);
    }

    public ModelRetryStrategy(int maxAttempts, double initialDelay, double maxDelay) {
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
    }

    @Override
    public void registerHooks(HookRegistry registry) {
        registry.register(AfterModelCallEvent.class, this::onAfterModelCall);
        registry.register(AfterInvocationEvent.class, event -> currentAttempt = 0);
    }

    private void onAfterModelCall(AfterModelCallEvent event) {
        if (event.getException() instanceof ModelThrottledException) {
            if (currentAttempt >= maxAttempts) {
                log.warn("Max retry attempts ({}) reached, giving up", maxAttempts);
                return;
            }

            double delay = Math.min(initialDelay * Math.pow(2, currentAttempt), maxDelay);
            currentAttempt++;
            log.info("Model throttled, retrying in {}s (attempt {}/{})", delay, currentAttempt, maxAttempts);

            try {
                Thread.sleep((long) (delay * 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            event.setRetry(true);
        } else if (event.getMessage() != null) {
            currentAttempt = 0;
        }
    }
}
