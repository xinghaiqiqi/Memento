package com.memento.service;

import com.memento.dto.TimelineNodeDTO;

import java.time.LocalDate;
import java.util.List;

public interface TimelineService {
    List<TimelineNodeDTO> getTimeline(Long userId, LocalDate startDate, LocalDate endDate, String viewMode);
}