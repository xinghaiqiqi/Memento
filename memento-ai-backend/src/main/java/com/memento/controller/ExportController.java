package com.memento.controller;

import com.memento.dto.Result;
import com.memento.entity.ExportTask;
import com.memento.mapper.ExportTaskMapper;
import com.memento.util.SecurityUtils;
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
    public Result<ExportTask> create(@RequestBody Map<String, Object> params) {
        ExportTask task = new ExportTask();
        task.setUserId(SecurityUtils.getCurrentUserId());
        task.setStatus("pending");
        task.setCreatedAt(LocalDateTime.now());
        exportTaskMapper.insert(task);
        
        // 模拟后台处理
        task.setStatus("completed");
        task.setFilePath("/exports/memento_" + task.getId() + ".pdf");
        task.setCompletedAt(LocalDateTime.now());
        exportTaskMapper.updateById(task);
        
        return Result.success(task);
    }

    @GetMapping("/status/{taskId}")
    public Result<ExportTask> getStatus(@PathVariable Long taskId) {
        ExportTask task = exportTaskMapper.selectById(taskId);
        return task != null ? Result.success(task) : Result.error("任务不存在");
    }
}
