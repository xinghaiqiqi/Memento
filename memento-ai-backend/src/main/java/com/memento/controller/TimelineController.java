package com.memento.controller;

import com.memento.dto.Result;
import com.memento.dto.TimelineNodeDTO;
import com.memento.service.TimelineService;
import com.memento.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/timeline")
public class TimelineController {

    @Autowired
    private TimelineService timelineService;

    @GetMapping
    public Result<List<TimelineNodeDTO>> getTimeline(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "day") String viewMode) {
        
        List<TimelineNodeDTO> nodes = timelineService.getTimeline(
                SecurityUtils.getCurrentUserId(), startDate, endDate, viewMode);
        
        return Result.success(nodes);
    }
}