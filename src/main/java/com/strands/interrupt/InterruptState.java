package com.strands.interrupt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InterruptState {

    private final Map<String, Interrupt> interrupts = new ConcurrentHashMap<>();
    private final Map<String, Object> context = new ConcurrentHashMap<>();
    private boolean activated;
    private int version;

    public void activate() {
        this.activated = true;
        this.version++;
    }

    public void deactivate() {
        this.activated = false;
        this.interrupts.clear();
        this.context.clear();
        this.version++;
    }

    public void addInterrupt(Interrupt interrupt) {
        interrupts.put(interrupt.getId(), interrupt);
    }

    public void resume(List<Map<String, Object>> responses) {
        if (!activated) {
            throw new IllegalStateException("Cannot resume: no active interrupts");
        }
        for (Map<String, Object> responseBlock : responses) {
            @SuppressWarnings("unchecked")
            Map<String, Object> interruptResponse = (Map<String, Object>) responseBlock.get("interruptResponse");
            if (interruptResponse == null) continue;

            String interruptId = (String) interruptResponse.get("interruptId");
            Object response = interruptResponse.get("response");

            Interrupt interrupt = interrupts.get(interruptId);
            if (interrupt == null) {
                throw new IllegalArgumentException("Unknown interrupt ID: " + interruptId);
            }
            interrupt.setResponse(response);
        }
        this.version++;
    }

    public boolean isActivated() {
        return activated;
    }

    public Map<String, Interrupt> getInterrupts() {
        return interrupts;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public int getVersion() {
        return version;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> interruptMaps = new LinkedHashMap<>();
        for (Map.Entry<String, Interrupt> entry : interrupts.entrySet()) {
            interruptMaps.put(entry.getKey(), entry.getValue().toMap());
        }
        map.put("interrupts", interruptMaps);
        map.put("context", context);
        map.put("activated", activated);
        map.put("version", version);
        return map;
    }
}
