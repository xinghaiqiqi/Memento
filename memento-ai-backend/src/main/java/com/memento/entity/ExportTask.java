package com.memento.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("export_tasks")
public class ExportTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String status; // pending/processing/completed/failed
    private String filePath;
    private String conditionJson;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
