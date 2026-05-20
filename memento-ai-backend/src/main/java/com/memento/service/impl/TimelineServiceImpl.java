package com.memento.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memento.dto.GalleryCardDTO;
import com.memento.dto.TimelineNodeDTO;
import com.memento.entity.Memory;
import com.memento.entity.Milestone;
import com.memento.mapper.MemoryMapper;
import com.memento.mapper.MilestoneMapper;
import com.memento.service.MemoryService;
import com.memento.service.TimelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimelineServiceImpl implements TimelineService {

    @Autowired
    private MemoryMapper memoryMapper;

    @Autowired
    private MilestoneMapper milestoneMapper;

    @Autowired
    private MemoryService memoryService;

    @Override
    public List<TimelineNodeDTO> getTimeline(Long userId, LocalDate startDate, LocalDate endDate, String viewMode) {
        LambdaQueryWrapper<Memory> memoryQuery = new LambdaQueryWrapper<>();
        memoryQuery.eq(Memory::getUserId, userId)
                   .eq(Memory::getIsDeleted, false);

        if (startDate != null) {
            memoryQuery.ge(Memory::getEventDate, startDate);
        }
        if (endDate != null) {
            memoryQuery.le(Memory::getEventDate, endDate);
        }

        memoryQuery.orderByAsc(Memory::getEventDate);

        List<Memory> memories = memoryMapper.selectList(memoryQuery);

        LambdaQueryWrapper<Milestone> milestoneQuery = new LambdaQueryWrapper<>();
        milestoneQuery.eq(Milestone::getUserId, userId);

        if (startDate != null) {
            milestoneQuery.ge(Milestone::getEventDate, startDate);
        }
        if (endDate != null) {
            milestoneQuery.le(Milestone::getEventDate, endDate);
        }

        milestoneQuery.orderByAsc(Milestone::getEventDate);

        List<Milestone> milestones = milestoneMapper.selectList(milestoneQuery);

        Map<Long, Milestone> milestoneMap = milestones.stream()
                .collect(Collectors.toMap(Milestone::getMemoryId, m -> m, (a, b) -> a));

        List<TimelineNodeDTO> nodes = new ArrayList<>();

        for (Memory memory : memories) {
            TimelineNodeDTO node = new TimelineNodeDTO();
            node.setId(memory.getId());
            node.setTitle(memory.getTitle());
            node.setContent(memory.getContent());
            node.setEventDate(memory.getEventDate());
            node.setDisplayDate(formatDate(memory.getEventDate(), viewMode));
            node.setSentimentScore(memory.getSentimentScore());
            node.setSentimentColor(GalleryCardDTO.getSentimentColor(memory.getSentimentScore()));
            node.setTags(memory.getTags());
            node.setCreatedAt(memory.getCreatedAt());

            Milestone milestone = milestoneMap.get(memory.getId());
            if (milestone != null) {
                node.setIsMilestone(true);
                TimelineNodeDTO.MilestoneInfo milestoneInfo = new TimelineNodeDTO.MilestoneInfo();
                milestoneInfo.setId(milestone.getId());
                milestoneInfo.setNodeType(milestone.getNodeType());
                milestoneInfo.setCustomTitle(milestone.getCustomTitle());
                milestoneInfo.setIsAuto(milestone.getIsAuto());
                node.setMilestone(milestoneInfo);
            } else {
                node.setIsMilestone(false);
            }

            nodes.add(node);
        }

        return nodes;
    }

    private String formatDate(LocalDate date, String viewMode) {
        if (date == null) return "";

        DateTimeFormatter formatter;
        switch (viewMode) {
            case "week":
                formatter = DateTimeFormatter.ofPattern("MM-dd");
                break;
            case "month":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                break;
            case "year":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                break;
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        }

        return date.format(formatter);
    }
}