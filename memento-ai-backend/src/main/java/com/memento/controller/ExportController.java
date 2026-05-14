package com.memento.controller;

import com.memento.entity.ExportTask;
import com.memento.mapper.ExportTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private ExportTaskMapper exportTaskMapper;

    @PostMapping("/create")
    public ExportTask create(@RequestBody Map<String, Object> params) {
        ExportTask task = new ExportTask();
        task.setUserId(1L);
        task.setStatus("pending");
        task.setCreatedAt(LocalDateTime.now());
        exportTaskMapper.insert(task);
        
        // 模拟后台处理
        task.setStatus("completed");
        task.setFilePath("/exports/memento_" + task.getId() + ".pdf");
        task.setCompletedAt(LocalDateTime.now());
        exportTaskMapper.updateById(task);
        
        return task;
    }

    @GetMapping("/status/{taskId}")
    public ExportTask getStatus(@PathVariable Long taskId) {
        return exportTaskMapper.selectById(taskId);
    }
}
