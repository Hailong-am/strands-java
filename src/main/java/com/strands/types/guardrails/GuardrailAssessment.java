package com.strands.types.guardrails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardrailAssessment {

    private TopicPolicy topicPolicy;
    private ContentPolicy contentPolicy;
    private WordPolicy wordPolicy;
    private SensitiveInformationPolicy sensitiveInformationPolicy;
    private ContextualGroundingPolicy contextualGroundingPolicy;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicPolicy {
        private List<Topic> topics;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Topic {
        private String action;
        private String name;
        private String type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentPolicy {
        private List<ContentFilter> filters;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentFilter {
        private String action;
        private String confidence;
        private String type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordPolicy {
        private List<CustomWord> customWords;
        private List<ManagedWord> managedWordLists;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomWord {
        private String action;
        private String match;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagedWord {
        private String action;
        private String match;
        private String type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensitiveInformationPolicy {
        private List<PIIEntity> piiEntities;
        private List<Regex> regexes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PIIEntity {
        private String action;
        private String match;
        private String type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Regex {
        private String action;
        private String match;
        private String name;
        private String regex;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextualGroundingPolicy {
        private List<ContextualGroundingFilter> filters;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextualGroundingFilter {
        private String action;
        private double score;
        private double threshold;
        private String type;
    }
}
