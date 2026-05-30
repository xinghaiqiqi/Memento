package com.memento.aisubsystem.controller;

import com.memento.aisubsystem.service.DialogueService;
import com.memento.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 心灵对话亭控制器（心灵回声子系统专属）。
 * 路径前缀 /api/ai-subsystem/* 与源代码 API 物理区分。
 * AI 调用全程走 MyDeepSeekClient（我自己的 DeepSeek），不触碰 AiUtils。
 */
@RestController
@RequestMapping("/api/ai-subsystem")
public class DialogueController {

    @Autowired
    private DialogueService dialogueService;

    /**
     * 共情对话。
     * 请求体：{ "message": "用户输入" }
     * 返回：{ reply: "AI回复" }
     */
    @PostMapping("/dialogue")
    public Result<Map<String, Object>> dialogue(@RequestBody Map<String, String> params) {
        String message = params.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Result.error("消息内容不能为空");
        }
        try {
            String reply = dialogueService.reply(message);
            Map<String, Object> data = new HashMap<>();
            data.put("reply", reply);
            return Result.success(data);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("对话服务暂时不可用：" + e.getMessage());
        }
    }

    /**
     * 结构化抽取：将对话凝练为记忆结构（title / sentimentScore / tags）。
     * 请求体：{ "userText": "...", "aiReply": "..." }
     */
    @PostMapping("/extract")
    public Result<Map<String, Object>> extract(@RequestBody Map<String, String> params) {
        String userText = params.getOrDefault("userText", "");
        String aiReply = params.getOrDefault("aiReply", "");
        if (userText.trim().isEmpty()) {
            return Result.error("用户内容不能为空");
        }
        try {
            Map<String, Object> data = dialogueService.extractMemory(userText, aiReply);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("记忆抽取失败：" + e.getMessage());
        }
    }
}
