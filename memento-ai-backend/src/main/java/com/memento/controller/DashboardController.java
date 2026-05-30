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
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long totalMemories = memoryService.count(new LambdaQueryWrapper<Memory>().eq(Memory::getUserId, userId));
            long totalMilestones = milestoneMapper.selectCount(new LambdaQueryWrapper<Milestone>().eq(Milestone::getUserId, userId));
            
            // 统计叙事生成，增加异常捕获以应对数据库字段未更新的情况
            long totalNarratives = 0;
            try {
                totalNarratives = narrativeMapper.selectCount(new LambdaQueryWrapper<GeneratedNarrative>().eq(GeneratedNarrative::getUserId, userId));
            } catch (Exception e) {
                System.err.println(">>> [Dashboard] Failed to count narratives: " + e.getMessage());
            }
            
            // 计算情感共鸣度 (积极情感占比)
            long positiveMemories = memoryService.count(new LambdaQueryWrapper<Memory>()
                    .eq(Memory::getUserId, userId)
                    .gt(Memory::getSentimentScore, 0.3));
            
            long totalForResonance = totalMemories + totalMilestones + totalNarratives;
            String resonance = totalForResonance == 0 ? "0%" : (positiveMemories * 100 / totalForResonance) + "%";

            stats.put("totalMemories", totalMemories);
            stats.put("totalMilestones", totalMilestones);
            stats.put("totalNarratives", totalNarratives);
            stats.put("resonance", resonance);
            
            return Result.success(stats);
        } catch (Exception e) {
            System.err.println(">>> [Dashboard] Failed to get stats: " + e.getMessage());
            return Result.error("获取统计数据失败，请检查数据库状态");
        }
    }

    @GetMapping("/recent")
    public Result<List<Memory>> getRecentMemories() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            List<Memory> list = memoryService.list(new LambdaQueryWrapper<Memory>()
                    .eq(Memory::getUserId, userId)
                    .orderByDesc(Memory::getEventDate)
                    .last("limit 5"));
            return Result.success(list);
        } catch (Exception e) {
            System.err.println(">>> [Dashboard] Failed to get recent memories: " + e.getMessage());
            return Result.error("获取近期记忆失败，请确保数据库已更新");
        }
    }
}
