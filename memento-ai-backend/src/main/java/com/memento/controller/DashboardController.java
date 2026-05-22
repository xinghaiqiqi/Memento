package com.memento.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memento.entity.GeneratedNarrative;
import com.memento.entity.Memory;
import com.memento.entity.Milestone;
import com.memento.mapper.GeneratedNarrativeMapper;
import com.memento.mapper.MilestoneMapper;
import com.memento.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private MilestoneMapper milestoneMapper;

    @Autowired
    private GeneratedNarrativeMapper narrativeMapper;

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        long totalMemories = memoryService.count(new LambdaQueryWrapper<Memory>().eq(Memory::getUserId, 1L));
        long totalMilestones = milestoneMapper.selectCount(new LambdaQueryWrapper<Milestone>().eq(Milestone::getUserId, 1L));
        long totalNarratives = narrativeMapper.selectCount(new LambdaQueryWrapper<GeneratedNarrative>().eq(GeneratedNarrative::getUserId, 1L));
        
        // 计算情感共鸣度 (积极情感占比)
        long positiveMemories = memoryService.count(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, 1L)
                .gt(Memory::getSentimentScore, 0.3));
        
        String resonance = totalMemories > 0 ? (positiveMemories * 100 / totalMemories) + "%" : "0%";

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMemories", totalMemories);
        stats.put("totalMilestones", totalMilestones);
        stats.put("totalNarratives", totalNarratives);
        stats.put("resonance", resonance);
        
        return stats;
    }

    @GetMapping("/recent")
    public List<Memory> getRecentMemories() {
        return memoryService.list(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, 1L)
                .orderByDesc(Memory::getEventDate)
                .last("limit 5"));
    }
}
