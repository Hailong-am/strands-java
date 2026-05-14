package com.strands.types;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResultContent {

    private String text;
    private Object json;

    public static ToolResultContent fromText(String text) {
        ToolResultContent content = new ToolResultContent();
        content.text = text;
        return content;
    }

    public static ToolResultContent fromJson(Object json) {
        ToolResultContent content = new ToolResultContent();
        content.json = json;
        return content;
    }

    public String getText() {
        return text;
    }

    public Object getJson() {
        return json;
    }
}
