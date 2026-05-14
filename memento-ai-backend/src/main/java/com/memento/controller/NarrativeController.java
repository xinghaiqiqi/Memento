package com.memento.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    public List<GeneratedNarrative> list() {
        return narrativeMapper.selectList(
                new LambdaQueryWrapper<GeneratedNarrative>().eq(GeneratedNarrative::getUserId, 1L).orderByDesc(GeneratedNarrative::getCreatedAt)
        );
    }

    @PostMapping("/generate")
    public GeneratedNarrative generate(@RequestBody Map<String, Object> params) {
        String style = (String) params.getOrDefault("style", "散文");
        String startDate = (String) params.get("startDate");
        String endDate = (String) params.get("endDate");

        LambdaQueryWrapper<Memory> query = new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, 1L)
                .orderByAsc(Memory::getEventDate);
        
        if (startDate != null) query.ge(Memory::getEventDate, startDate);
        if (endDate != null) query.le(Memory::getEventDate, endDate);

        List<Memory> memories = memoryService.list(query);
        String memoriesList = memories.stream()
                .map(m -> m.getEventDate() + " - " + m.getTitle() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        String content = aiUtils.generateNarrative(memoriesList, style);

        GeneratedNarrative narrative = new GeneratedNarrative();
        narrative.setUserId(1L);
        narrative.setTitle("我的记忆叙事 - " + LocalDateTime.now().toLocalDate());
        narrative.setContent(content);
        narrative.setWordCount(content.length());
        narrative.setCreatedAt(LocalDateTime.now());
        
        narrativeMapper.insert(narrative);
        return narrative;
    }
}
