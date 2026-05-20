package com.memento.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TimelineNodeDTO {
    private Long id;
    private String title;
    private String content;
    private LocalDate eventDate;
    private String displayDate;
    private BigDecimal sentimentScore;
    private String sentimentColor;
    private String tags;
    
    private Boolean isMilestone;
    private MilestoneInfo milestone;
    private LocalDateTime createdAt;

    @Data
    public static class MilestoneInfo {
        private Long id;
        private String nodeType;
        private String customTitle;
        private Boolean isAuto;
    }
}