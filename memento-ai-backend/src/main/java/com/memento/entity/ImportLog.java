package com.memento.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("import_logs")
public class ImportLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String filename;
    private Integer successCount;
    private Integer failCount;
    private String errorMessage;
    private LocalDateTime importedAt;
}
