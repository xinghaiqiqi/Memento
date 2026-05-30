package com.memento.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memento.dto.Result;
import com.memento.entity.GeneratedNarrative;
import com.memento.entity.Memory;
import com.memento.mapper.GeneratedNarrativeMapper;
import com.memento.service.MemoryService;
import com.memento.util.AiUtils;
import com.memento.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/narratives")
public class NarrativeController {

    @Autowired
    private GeneratedNarrativeMapper narrativeMapper;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private AiUtils aiUtils;

    @GetMapping("/list")
    public Result<List<GeneratedNarrative>> list(@RequestParam(required = false) String keyword) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            LambdaQueryWrapper<GeneratedNarrative> query = new LambdaQueryWrapper<>();
            query.eq(GeneratedNarrative::getUserId, userId);
            if (keyword != null && !keyword.isEmpty()) {
                query.like(GeneratedNarrative::getTitle, keyword).or().like(GeneratedNarrative::getContent, keyword);
            }
            query.orderByDesc(GeneratedNarrative::getCreatedAt);
            List<GeneratedNarrative> list = narrativeMapper.selectList(query);
            return Result.success(list);
        } catch (Exception e) {
            System.err.println(">>> [Narrative] Failed to list narratives: " + e.getMessage());
            // 如果表结构未更新，返回空列表而不是报错
            return Result.success(new java.util.ArrayList<>());
        }
    }

    @PostMapping("/generate")
    public Result<GeneratedNarrative> generate(@RequestBody Map<String, Object> params) {
        Long userId = SecurityUtils.getCurrentUserId();
        String style = (String) params.getOrDefault("style", "prose");
        List<String> dateRange = (List<String>) params.get("dateRange");
        
        LambdaQueryWrapper<Memory> query = new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsDeleted, false);
        
        if (dateRange != null && dateRange.size() == 2) {
            query.ge(Memory::getEventDate, dateRange.get(0))
                 .le(Memory::getEventDate, dateRange.get(1));
        }
        
        query.orderByDesc(Memory::getEventDate).last("limit 20");
        
        // 获取匹配时间段的记忆
        List<Memory> memories = memoryService.list(query);
        
        if (memories.isEmpty()) {
            return Result.error("所选时间段内没有足够的记忆来编织故事");
        }

        String memoryText = memories.stream()
                .map(m -> (m.getTitle() != null ? m.getTitle() : "无标题") + ": " + (m.getContent() != null ? m.getContent() : "无内容"))
                .collect(Collectors.joining("\n"));
        
        try {
            String content = aiUtils.generateNarrative(memoryText, style);
            if (content == null || content.isEmpty()) {
                return Result.error("AI 编织引擎返回了空卷轴，请稍后重试");
            }
            
            GeneratedNarrative narrative = new GeneratedNarrative();
            narrative.setUserId(userId);
            narrative.setType(style);
            narrative.setContent(content);
            narrative.setTitle("记忆篇章：" + (memories.get(0).getEventDate() != null ? memories.get(0).getEventDate().toString() : "未知日期"));
            narrative.setCreatedAt(LocalDateTime.now());
            narrative.setWordCount(content.length());
            
            return Result.success(narrative);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("编织仪式意外中断：" + e.getMessage());
        }
    }

    @PostMapping("/save")
    public Result<GeneratedNarrative> save(@RequestBody GeneratedNarrative narrative) {
        try {
            narrative.setUserId(SecurityUtils.getCurrentUserId());
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
