package com.memento.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TopicClusterDTO {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private List<Long> memoryIds;
    private List<MemorySummary> memories;
    private Integer memoryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    public static class MemorySummary {
        private Long id;
        private String title;
        private String content;
        private String eventDate;
        private Double sentimentScore;
        private String photoUrl;
    }
}