package com.memento.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GalleryCardDTO {
    private Long id;
    private String title;
    private String summary;
    private LocalDate eventDate;
    private String tags;
    private BigDecimal sentimentScore;
    private String sentimentColor;
    private String topicName;
    private Long topicId;
    private LocalDateTime createdAt;

    public static String getSentimentColor(BigDecimal score) {
        if (score == null) return "#9ca3af";
        double s = score.doubleValue();
        if (s > 0.5) return "#ef4444";
        if (s > 0.2) return "#f97316";
        if (s > -0.2) return "#eab308";
        if (s > -0.5) return "#84cc16";
        return "#3b82f6";
    }
}