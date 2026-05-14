package com.strands.interrupt;

import java.util.LinkedHashMap;
import java.util.Map;

public class Interrupt {

    private final String id;
    private final String name;
    private Object reason;
    private Object response;

    public Interrupt(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Interrupt(String id, String name, Object reason) {
        this.id = id;
        this.name = name;
        this.reason = reason;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Object getReason() {
        return reason;
    }

    public void setReason(Object reason) {
        this.reason = reason;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        if (reason != null) map.put("reason", reason);
        if (response != null) map.put("response", response);
        return map;
    }
}
