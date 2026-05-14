package com.memento.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("milestones")
public class Milestone {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long memoryId;
    private String nodeType;
    private String customTitle;
    private LocalDate eventDate;
    private Boolean isAuto;
    private LocalDateTime createdAt;
}
