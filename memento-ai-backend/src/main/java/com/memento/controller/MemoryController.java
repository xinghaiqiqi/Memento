package com.memento.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memento.entity.Memory;
import com.memento.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {

    @Autowired
    private MemoryService memoryService;

    @GetMapping
    public Page<Memory> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String sentimentType
    ) {
        Page<Memory> page = new Page<>(current, size);
        LambdaQueryWrapper<Memory> query = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            query.and(q -> q.like(Memory::getTitle, keyword).or().like(Memory::getContent, keyword));
        }
        if (startDate != null) {
            query.ge(Memory::getEventDate, startDate);
        }
        if (endDate != null) {
            query.le(Memory::getEventDate, endDate);
        }
        if (sentimentType != null) {
            if ("positive".equals(sentimentType)) query.gt(Memory::getSentimentScore, 0.3);
            else if ("negative".equals(sentimentType)) query.lt(Memory::getSentimentScore, -0.3);
            else if ("neutral".equals(sentimentType)) query.between(Memory::getSentimentScore, -0.3, 0.3);
        }
        
        query.orderByDesc(Memory::getEventDate);
        return memoryService.page(page, query);
    }

    @PostMapping
    public boolean save(@RequestBody Memory memory) {
        return memoryService.saveMemory(memory);
    }

    @GetMapping("/{id}")
    public Memory getById(@PathVariable Long id) {
        return memoryService.getById(id);
    }

    @PutMapping("/{id}")
    public boolean update(@PathVariable Long id, @RequestBody Memory memory) {
        memory.setId(id);
        return memoryService.updateMemory(memory);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return memoryService.removeById(id);
    }
}
