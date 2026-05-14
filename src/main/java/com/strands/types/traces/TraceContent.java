package com.strands.types.traces;

import com.strands.types.guardrails.GuardrailAssessment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceContent {

    private GuardrailTrace guardrail;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuardrailTrace {
        private Map<String, GuardrailAssessment> inputAssessment;
        private List<String> modelOutput;
        private Map<String, List<GuardrailAssessment>> outputAssessments;
    }
}
