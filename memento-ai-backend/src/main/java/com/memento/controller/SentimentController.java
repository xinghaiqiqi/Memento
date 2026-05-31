package com.memento.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.memento.dto.Result;
import com.memento.entity.Memory;
import com.memento.service.MemoryService;
import com.memento.util.AiUtils;
import com.memento.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sentiment")
public class SentimentController {

    @Autowired
    private MemoryService memoryService;
    
    @Autowired
    private AiUtils aiUtils;

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        Long userId = SecurityUtils.getCurrentUserId();
        
        // 总体统计 - 添加用户ID过滤
        long total = memoryService.count(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsDeleted, false));
        stats.put("totalCount", total);
        
        // 情感分布 - 添加用户ID过滤
        long positive = memoryService.count(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsDeleted, false)
                .gt(Memory::getSentimentScore, 0.3));
        long negative = memoryService.count(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsDeleted, false)
                .lt(Memory::getSentimentScore, -0.3));
        long neutral = total - positive - negative;
        
        stats.put("positiveCount", positive);
        stats.put("negativeCount", negative);
        stats.put("neutralCount", neutral);
        
        return Result.success(stats);
    }

    @GetMapping("/timeseries")
    public Result<List<Map<String, Object>>> getTimeSeries() {
        Long userId = SecurityUtils.getCurrentUserId();
        
        // 返回最近 10 条记忆的情感变化 - 添加用户ID过滤
        List<Map<String, Object>> list = memoryService.list(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsDeleted, false)
                .orderByDesc(Memory::getEventDate)
                .last("limit 10"))
                .stream()
                .map(m -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("date", m.getEventDate());
                    item.put("score", m.getSentimentScore());
                    return item;
                })
                .collect(Collectors.toList());
        return Result.success(list);
    }

    @GetMapping("/dates")
    public Result<List<String>> getAvailableDates() {
        Long userId = SecurityUtils.getCurrentUserId();

        List<String> dates = memoryService.list(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsDeleted, false)
                .isNotNull(Memory::getEventDate)
                .orderByDesc(Memory::getEventDate))
                .stream()
                .map(m -> m.getEventDate().toString())
                .distinct()
                .collect(Collectors.toList());

        return Result.success(dates);
    }

    @GetMapping("/memories")
    public Result<List<Map<String, Object>>> getMemoriesByDate(@RequestParam String date) {
        LocalDate localDate = parseDateParam(date);
        if (localDate == null) {
            return Result.error("日期格式无效，请使用 yyyy-MM-dd");
        }

        Long userId = SecurityUtils.getCurrentUserId();

        List<Memory> memoryList = memoryService.list(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsDeleted, false)
                .eq(Memory::getEventDate, localDate));

        List<Map<String, Object>> memories = memoryList.stream()
                .map(m -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", m.getId());
                    item.put("title", m.getTitle());
                    item.put("content", m.getContent());
                    item.put("eventDate", m.getEventDate());
                    item.put("sentimentScore", m.getSentimentScore() != null ? m.getSentimentScore().doubleValue() : null);
                    item.put("tags", m.getTags());
                    item.put("photoUrl", m.getPhotoUrl());
                    return item;
                })
                .collect(Collectors.toList());

        return Result.success(memories);
    }

    private LocalDate parseDateParam(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        String normalized = date.trim();
        if (normalized.length() >= 10) {
            normalized = normalized.substring(0, 10);
        }
        try {
            return LocalDate.parse(normalized);
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/advice")
    public Result<Map<String, Object>> getAdvice(
            @RequestParam String memoryContent,
            @RequestParam(required = false) String question) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String prompt = "根据以下记忆内容，给出一些人生建议或见解（类似星座/MBTI风格）：\n\n";
            prompt += "记忆内容：" + memoryContent + "\n\n";
            
            if (question != null && !question.isEmpty()) {
                prompt += "用户问题：" + question + "\n\n";
            }
            
            prompt += "请用温暖、睿智的语言给出建议，包含一些象征性的比喻或意象。";
            
            String advice = aiUtils.chat(prompt);
            result.put("advice", advice);
            result.put("success", true);
        } catch (Exception e) {
            result.put("advice", "很抱歉，暂时无法获取建议，请稍后再试。");
            result.put("success", false);
        }
        
        return Result.success(result);
    }

    @GetMapping("/debug")
    public Result<Map<String, Object>> debug() {
        Map<String, Object> debugInfo = new HashMap<>();
        Long userId = SecurityUtils.getCurrentUserId();
        
        // 总记录数
        long totalCount = memoryService.count(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId));
        debugInfo.put("totalCount", totalCount);
        
        // 未删除记录数
        long activeCount = memoryService.count(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsDeleted, false));
        debugInfo.put("activeCount", activeCount);
        
        // 所有日期（按时间倒序，取前10条）
        List<Memory> recentMemories = memoryService.list(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsDeleted, false)
                .orderByDesc(Memory::getEventDate)
                .last("limit 10"));
        
        List<String> dates = recentMemories.stream()
                .map(m -> m.getEventDate() != null ? m.getEventDate().toString() : "null")
                .collect(Collectors.toList());
        debugInfo.put("recentDates", dates);
        
        // 示例记忆
        if (!recentMemories.isEmpty()) {
            Memory sample = recentMemories.get(0);
            Map<String, Object> sampleData = new HashMap<>();
            sampleData.put("id", sample.getId());
            sampleData.put("title", sample.getTitle());
            sampleData.put("eventDate", sample.getEventDate());
            sampleData.put("sentimentScore", sample.getSentimentScore());
            sampleData.put("photoUrl", sample.getPhotoUrl());
            debugInfo.put("sampleMemory", sampleData);
        }
        
        return Result.success(debugInfo);
    }
}
