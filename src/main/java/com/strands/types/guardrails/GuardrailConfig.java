package com.strands.types.guardrails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardrailConfig {

    private String guardrailIdentifier;
    private String guardrailVersion;
    private String streamProcessingMode;
    private String trace;
}
