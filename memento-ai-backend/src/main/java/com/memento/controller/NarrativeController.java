package com.memento.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memento.dto.Result;
import com.memento.entity.GeneratedNarrative;
import com.memento.entity.Memory;
import com.memento.mapper.GeneratedNarrativeMapper;
import com.memento.service.MemoryService;
import com.memento.util.AiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/narrative")
public class NarrativeController {

    @Autowired
    private GeneratedNarrativeMapper narrativeMapper;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private AiUtils aiUtils;

    @GetMapping("/list")
    public Result<List<GeneratedNarrative>> list(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<GeneratedNarrative> query = new LambdaQueryWrapper<GeneratedNarrative>()
                .eq(GeneratedNarrative::getUserId, 1L)
                .orderByDesc(GeneratedNarrative::getCreatedAt);
        
        if (keyword != null && !keyword.isEmpty()) {
            query.and(q -> q.like(GeneratedNarrative::getTitle, keyword)
                    .or()
                    .like(GeneratedNarrative::getContent, keyword));
        }
        
        return Result.success(narrativeMapper.selectList(query));
    }

    @PostMapping("/generate")
    public Result<GeneratedNarrative> generate(@RequestBody Map<String, Object> params) {
        try {
            String style = (String) params.getOrDefault("style", "prose");
            // 映射前端 key 为中文风格描述
            String styleName = switch (style) {
                case "prose" -> "优美散文";
                case "diary" -> "私密日记";
                case "novel" -> "小说纪实";
                default -> style;
            };
            
            @SuppressWarnings("unchecked")
            List<String> dateRange = (List<String>) params.get("dateRange");
            String startDate = null;
            String endDate = null;
            if (dateRange != null && dateRange.size() == 2) {
                startDate = dateRange.get(0);
                endDate = dateRange.get(1);
            }

            LambdaQueryWrapper<Memory> query = new LambdaQueryWrapper<Memory>()
                    .eq(Memory::getUserId, 1L)
                    .orderByAsc(Memory::getEventDate);
            
            if (startDate != null) query.ge(Memory::getEventDate, startDate);
            if (endDate != null) query.le(Memory::getEventDate, endDate);

            List<Memory> memories = memoryService.list(query);
            System.out.println(">>> [Narrative] Found " + memories.size() + " memories for range: " + startDate + " to " + endDate);
            
            if (memories.isEmpty()) {
                return Result.error("所选时间段内没有记忆片段，无法编织叙事。请尝试扩大时间范围。");
            }

            String memoriesList = memories.stream()
                    .map(m -> String.format("[%s] %s: %s", m.getEventDate(), m.getTitle(), m.getContent()))
                    .collect(Collectors.joining("\n"));

            String content = aiUtils.generateNarrative(memoriesList, styleName);

            GeneratedNarrative narrative = new GeneratedNarrative();
            narrative.setUserId(1L);
            narrative.setTitle("记忆叙事 - " + (startDate != null ? startDate : "全时期") + " 至 " + (endDate != null ? endDate : "今"));
            narrative.setContent(content);
            narrative.setWordCount(content.length());
            narrative.setCreatedAt(LocalDateTime.now());
            
            return Result.success(narrative);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("编织失败: " + e.getMessage());
        }
    }

    @PostMapping("/save")
    public Result<GeneratedNarrative> save(@RequestBody GeneratedNarrative narrative) {
        try {
            narrative.setUserId(1L);
            if (narrative.getCreatedAt() == null) {
                narrative.setCreatedAt(LocalDateTime.now());
            }
            if (narrative.getId() != null) {
                narrativeMapper.updateById(narrative);
            } else {
                narrativeMapper.insert(narrative);
            }
            return Result.success(narrative);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    public Result<GeneratedNarrative> update(@RequestBody GeneratedNarrative narrative) {
        try {
            narrativeMapper.updateById(narrative);
            return Result.success(narrative);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        narrativeMapper.deleteById(id);
        return Result.success(null);
    }
}
