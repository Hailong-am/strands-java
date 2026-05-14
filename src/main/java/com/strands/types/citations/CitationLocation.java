package com.strands.types.citations;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public sealed interface CitationLocation
        permits CitationLocation.DocumentChar, CitationLocation.DocumentPage,
        CitationLocation.DocumentChunk, CitationLocation.SearchResult,
        CitationLocation.Web {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    final class DocumentChar implements CitationLocation {
        private int documentIndex;
        private int start;
        private int end;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    final class DocumentPage implements CitationLocation {
        private int documentIndex;
        private int start;
        private int end;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    final class DocumentChunk implements CitationLocation {
        private int documentIndex;
        private int start;
        private int end;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    final class SearchResult implements CitationLocation {
        private int searchResultIndex;
        private int start;
        private int end;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    final class Web implements CitationLocation {
        private String url;
        private String domain;
    }
}
