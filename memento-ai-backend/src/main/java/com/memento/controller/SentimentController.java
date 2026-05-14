package com.memento.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.memento.entity.Memory;
import com.memento.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sentiment")
public class SentimentController {

    @Autowired
    private MemoryService memoryService;

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总体统计
        long total = memoryService.count();
        stats.put("totalCount", total);
        
        // 情感分布
        long positive = memoryService.count(new QueryWrapper<Memory>().gt("sentiment_score", 0.3));
        long negative = memoryService.count(new QueryWrapper<Memory>().lt("sentiment_score", -0.3));
        long neutral = total - positive - negative;
        
        stats.put("positiveCount", positive);
        stats.put("negativeCount", negative);
        stats.put("neutralCount", neutral);
        
        return stats;
    }

    @GetMapping("/timeseries")
    public List<Map<String, Object>> getTimeSeries() {
        // 简化：返回最近 10 条记忆的情感变化
        return memoryService.list(new QueryWrapper<Memory>().orderByDesc("event_date").last("limit 10"))
                .stream()
                .map(m -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("date", m.getEventDate());
                    item.put("score", m.getSentimentScore());
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
