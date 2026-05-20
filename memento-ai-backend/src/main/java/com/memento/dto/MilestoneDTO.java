package com.memento.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MilestoneDTO {
    private Long id;
    private Long userId;
    private Long memoryId;
    private String nodeType;
    private String customTitle;
    private LocalDate eventDate;
    private Boolean isAuto;
    
    private MemoryDetail memory;
    private LocalDateTime createdAt;

    @Data
    public static class MemoryDetail {
        private Long id;
        private String title;
        private String content;
        private LocalDate eventDate;
        private BigDecimal sentimentScore;
    }
}