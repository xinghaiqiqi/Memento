package com.memento.aisubsystem.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.memento.aisubsystem.client.MyDeepSeekClient;
import com.memento.aisubsystem.service.DialogueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 心灵对话亭服务实现。
 * 全程使用本子系统专属的 MyDeepSeekClient，不触碰源代码中的 AiUtils。
 */
@Service
public class DialogueServiceImpl implements DialogueService {

    @Autowired
    private MyDeepSeekClient deepSeekClient;

    /** 倾听者人设：被听见 + 被理解 */
    private static final String SYSTEM_PROMPT =
            "你是「心灵对话亭」里一位温暖、真诚、善于倾听的陪伴者。" +
            "用户会向你倾诉情绪、事件或零散的想法。请遵循：" +
            "1. 先接住情绪，表达理解与共情，不评判、不说教；" +
            "2. 用简短的话点出你听到的核心状态（像一面温柔的镜子）；" +
            "3. 如果合适，给一点轻柔的反馈或陪伴性的话语，不要长篇大论；" +
            "4. 语气自然、有温度，像一个真正在乎对方的朋友。";

    @Override
    public String reply(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "我在这里，慢慢说，我在听。";
        }
        return deepSeekClient.chat(SYSTEM_PROMPT, message.trim());
    }

    @Override
    public Map<String, Object> extractMemory(String userText, String aiReply) {
        Map<String, Object> result = new HashMap<>();
        // 兜底默认值，保证即使解析失败也能存储
        result.put("title", buildFallbackTitle(userText));
        result.put("sentimentScore", 0.0);
        result.put("tags", "");

        String prompt = "请把下面这段心灵对话凝练为一条可存储的「记忆」，只返回严格的 JSON，" +
                "不要任何解释文字。JSON 字段要求：\n" +
                "- title：为这段记忆取一个简短的标题（不超过 20 字）\n" +
                "- sentimentScore：用户当前情绪分值，范围 -1.0 到 1.0，积极为正，消极为负\n" +
                "- tags：从内容中提炼 1~3 个关键词，用英文逗号分隔的字符串\n" +
                "格式示例：{\"title\":\"...\",\"sentimentScore\":0.5,\"tags\":\"工作,焦虑\"}\n\n" +
                "【用户表达】" + userText + "\n【对话回应】" + aiReply;

        try {
            String raw = deepSeekClient.chat(null, prompt);
            JSONObject json = JSON.parseObject(raw);
            if (json != null) {
                if (json.containsKey("title") && json.getString("title") != null
                        && !json.getString("title").trim().isEmpty()) {
                    result.put("title", json.getString("title").trim());
                }
                if (json.containsKey("sentimentScore")) {
                    result.put("sentimentScore", json.getDoubleValue("sentimentScore"));
                }
                if (json.containsKey("tags") && json.getString("tags") != null) {
                    result.put("tags", json.getString("tags").trim());
                }
            }
        } catch (Exception e) {
            // 解析失败则使用兜底值，不影响保存
            System.err.println(">>> [心灵对话亭] 结构化抽取失败，使用兜底值: " + e.getMessage());
        }
        return result;
    }

    private String buildFallbackTitle(String userText) {
        if (userText == null || userText.trim().isEmpty()) {
            return "一段心灵对话";
        }
        String t = userText.trim();
        return t.length() > 18 ? t.substring(0, 18) + "..." : t;
    }
}
