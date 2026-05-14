package com.strands.types.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaContent {

    private String format;
    private String name;
    private byte[] source;
    private String sourceType;
}
