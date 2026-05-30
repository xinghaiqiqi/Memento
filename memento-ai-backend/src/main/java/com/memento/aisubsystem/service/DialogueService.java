package com.memento.aisubsystem.service;

import java.util.Map;

/**
 * 心灵对话亭服务接口（心灵回声子系统专属，独立于源代码 AI 逻辑）
 */
public interface DialogueService {

    /**
     * 共情对话：接住用户情绪并给予理解性回应。
     *
     * @param message 用户输入
     * @return AI 回复文本
     */
    String reply(String message);

    /**
     * 结构化抽取：将一段对话内容凝练为可存储的记忆结构。
     *
     * @param userText 用户原始表达
     * @param aiReply  AI 的回应
     * @return 含 title / sentimentScore / tags 的结构化数据
     */
    Map<String, Object> extractMemory(String userText, String aiReply);
}
