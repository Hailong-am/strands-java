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
public class CitationsContentBlock {

    private List<Citation> citations;
    private List<Citation.CitationGeneratedContent> content;
}
