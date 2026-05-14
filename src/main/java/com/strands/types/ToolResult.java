package com.strands.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResult {

    public enum Status {
        SUCCESS("success"),
        ERROR("error");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @JsonProperty("toolUseId")
    private String toolUseId;
    private Status status;
    private List<ToolResultContent> content;

    public static ToolResult success(String toolUseId, String text) {
        return new ToolResult(toolUseId, Status.SUCCESS, List.of(ToolResultContent.fromText(text)));
    }

    public static ToolResult error(String toolUseId, String errorMessage) {
        return new ToolResult(toolUseId, Status.ERROR, List.of(ToolResultContent.fromText(errorMessage)));
    }
}
