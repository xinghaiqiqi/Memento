package com.memento.aisubsystem.controller;

import com.memento.aisubsystem.entity.FutureLetter;
import com.memento.aisubsystem.service.FutureLetterService;
import com.memento.dto.Result;
import com.memento.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 未来邮局控制器（心灵回声子系统专属）
 * 路径前缀 /api/ai-subsystem/letters，与源码 API 物理区分
 */
@RestController
@RequestMapping("/api/ai-subsystem/letters")
public class FutureLetterController {

    @Autowired
    private FutureLetterService futureLetterService;

    /** 获取当前用户所有信件（含动态 status） */
    @GetMapping
    public Result<List<FutureLetter>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(futureLetterService.listByUser(userId));
    }

    /** 寄出一封信 */
    @PostMapping
    public Result<FutureLetter> save(@RequestBody FutureLetter letter) {
        if (letter.getTitle() == null || letter.getTitle().trim().isEmpty()) {
            return Result.error("信件标题不能为空");
        }
        if (letter.getContent() == null || letter.getContent().trim().isEmpty()) {
            return Result.error("信件内容不能为空");
        }
        if (letter.getTargetDate() == null) {
            return Result.error("请选择信件的送达时间");
        }
        letter.setUserId(SecurityUtils.getCurrentUserId());
        return Result.success(futureLetterService.saveLetter(letter));
    }

    /** 撤回（删除）一封信 */
    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean ok = futureLetterService.removeLetter(id, userId);
        return ok ? Result.success(true) : Result.error("删除失败或无权限");
    }
}
