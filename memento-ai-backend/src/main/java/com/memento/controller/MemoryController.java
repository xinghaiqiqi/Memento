package com.memento.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memento.dto.Result;
import com.memento.entity.Memory;
import com.memento.service.MemoryService;
import com.memento.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {

    @Autowired
    private MemoryService memoryService;

    @GetMapping
    public Result<Page<Memory>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String sentimentType
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<Memory> page = new Page<>(current, size);
        LambdaQueryWrapper<Memory> query = new LambdaQueryWrapper<>();
        
        query.eq(Memory::getUserId, userId);
        
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
        return Result.success(memoryService.page(page, query));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Memory memory) {
        boolean success = memoryService.saveMemory(memory);
        return success ? Result.success(true) : Result.error("保存失败");
    }

    @GetMapping("/{id}")
    public Result<Memory> getById(@PathVariable Long id) {
        Memory memory = memoryService.getById(id);
        return memory != null ? Result.success(memory) : Result.error("记忆不存在");
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Memory memory) {
        memory.setId(id);
        boolean success = memoryService.updateMemory(memory);
        return success ? Result.success(true) : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = memoryService.removeById(id);
        return success ? Result.success(true) : Result.error("删除失败");
    }
}
