package com.strands.types.citations;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Citation {

    private CitationLocation location;
    private List<CitationSourceContent> sourceContent;
    private String title;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitationSourceContent {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitationGeneratedContent {
        private String text;
    }
}
