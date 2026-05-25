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

    @GetMapping
    public Result<List<GeneratedNarrative>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<GeneratedNarrative> list = narrativeMapper.selectList(new LambdaQueryWrapper<GeneratedNarrative>()
                .eq(GeneratedNarrative::getUserId, userId)
                .orderByDesc(GeneratedNarrative::getCreatedAt));
        return Result.success(list);
    }

    @PostMapping("/generate")
    public Result<GeneratedNarrative> generate(@RequestBody Map<String, Object> params) {
        Long userId = SecurityUtils.getCurrentUserId();
        String type = (String) params.getOrDefault("type", "weekly");
        
        // 获取最近的记忆
        List<Memory> memories = memoryService.list(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsDeleted, false)
                .orderByDesc(Memory::getEventDate)
                .last("limit 10"));
        
        if (memories.isEmpty()) {
            return Result.error("没有足够的记忆来生成叙事");
        }

        String memoryText = memories.stream()
                .map(m -> m.getTitle() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
        
        String content = aiUtils.generateNarrative(memoryText, type);
        
        GeneratedNarrative narrative = new GeneratedNarrative();
        narrative.setUserId(userId);
        narrative.setType(type);
        narrative.setContent(content);
        narrative.setTitle(type.equals("weekly") ? "周度情感回响" : "记忆深度解析");
        narrative.setCreatedAt(LocalDateTime.now());
        
        narrativeMapper.insert(narrative);
        return Result.success(narrative);
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
