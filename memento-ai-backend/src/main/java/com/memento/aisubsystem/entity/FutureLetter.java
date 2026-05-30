package com.memento.aisubsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 未来邮局信件实体（心灵回声子系统专属，独立于现有表）
 */
@Data
@TableName("future_letters")
public class FutureLetter {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID，与 memories 表保持一致，默认 1L */
    private Long userId;

    /** 信件标题 */
    private String title;

    /** 信件正文 */
    private String content;

    /** AI 生成的寄语（前端生成后传入，后端存储） */
    private String echo;

    /** 解封时间：当 now >= targetDate 时自动解锁 */
    private LocalDateTime targetDate;

    /**
     * 状态：locked（封存中）/ unlocked（已抵达）
     * 由后端查询时根据 targetDate 与当前时间动态计算，不依赖手动更新
     */
    private String status;

    private LocalDateTime createdAt;
}
