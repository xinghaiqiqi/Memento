package com.memento.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memento.dto.Result;
import com.memento.entity.GeneratedNarrative;
import com.memento.entity.Memory;
import com.memento.entity.Milestone;
import com.memento.mapper.GeneratedNarrativeMapper;
import com.memento.mapper.MilestoneMapper;
import com.memento.service.MemoryService;
import com.memento.util.SecurityUtils;
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
    public Result<Map<String, Object>> getStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        
        long totalMemories = memoryService.count(new LambdaQueryWrapper<Memory>().eq(Memory::getUserId, userId));
        long totalMilestones = milestoneMapper.selectCount(new LambdaQueryWrapper<Milestone>().eq(Milestone::getUserId, userId));
        long totalNarratives = narrativeMapper.selectCount(new LambdaQueryWrapper<GeneratedNarrative>().eq(GeneratedNarrative::getUserId, userId));
        
        // 计算情感共鸣度 (积极情感占比)
        long positiveMemories = memoryService.count(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .gt(Memory::getSentimentScore, 0.3));
        
        long totalForResonance = totalMemories + totalMilestones + totalNarratives;
        String resonance = totalForResonance == 0 ? "0%" : (positiveMemories * 100 / totalForResonance) + "%";

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMemories", totalMemories);
        stats.put("totalMilestones", totalMilestones);
        stats.put("totalNarratives", totalNarratives);
        stats.put("resonance", resonance);
        
        return Result.success(stats);
    }

    @GetMapping("/recent")
    public Result<List<Memory>> getRecentMemories() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Memory> list = memoryService.list(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .orderByDesc(Memory::getEventDate)
                .last("limit 5"));
        return Result.success(list);
    }
}
