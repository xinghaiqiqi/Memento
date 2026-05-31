package com.memento.aisubsystem.controller;

import com.memento.aisubsystem.client.MyDeepSeekClient;
import com.memento.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 心灵回声子系统 AI 洞察接口
 * 复用 MyDeepSeekClient（我自己的 DeepSeek），不触碰源码 AiUtils
 */
@RestController
@RequestMapping("/api/ai-subsystem/insight")
public class InsightController {

    @Autowired
    private MyDeepSeekClient deepSeekClient;

    /**
     * 灵魂关键词深度洞察
     * 请求体：{ keywords: [{word, count}], positive: n, neutral: n, negative: n, total: n }
     */
    @PostMapping("/keywords")
    public Result<String> keywordsInsight(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> keywords = (List<Map<String, Object>>) params.get("keywords");
            int positive = toInt(params.get("positive"));
            int neutral  = toInt(params.get("neutral"));
            int negative = toInt(params.get("negative"));
            int total    = toInt(params.get("total"));

            if (keywords == null || keywords.isEmpty()) {
                return Result.error("关键词数据为空");
            }

            StringBuilder kw = new StringBuilder();
            for (int i = 0; i < Math.min(keywords.size(), 10); i++) {
                Map<String, Object> item = keywords.get(i);
                kw.append("「").append(item.get("word")).append("」×").append(item.get("count"));
                if (i < keywords.size() - 1) kw.append("、");
            }

            String prompt = "你是一位温暖而深刻的心理洞察师。请根据以下用户记忆数据，用 2-3 句话生成一段有温度的深度洞察，" +
                    "帮助用户理解自己的内心状态。语言要诗意、真诚，不要说教，不要列清单，直接输出洞察文字。\n\n" +
                    "用户记忆关键词（词 × 出现次数）：" + kw + "\n" +
                    "情感分布：积极 " + positive + " 条，中性 " + neutral + " 条，消极 " + negative + " 条，共 " + total + " 条记忆。";

            String insight = deepSeekClient.chat(null, prompt);
            return Result.success(insight);
        } catch (Exception e) {
            return Result.error("AI 洞察生成失败：" + e.getMessage());
        }
    }

    /**
     * 时光勋章成长寄语
     * 请求体：{ unlocked: ["勋章名"], total: n, avgSentiment: 0.x }
     */
    @PostMapping("/badges")
    public Result<String> badgesInsight(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<String> unlocked = (List<String>) params.get("unlocked");
            int total = toInt(params.get("total"));
            double avg = toDouble(params.get("avgSentiment"));

            if (unlocked == null || unlocked.isEmpty()) {
                return Result.error("暂无已解锁勋章");
            }

            String prompt = "你是一位温暖的成长见证者。请根据用户已解锁的时光勋章，用 2-3 句话生成一段个性化的成长寄语，" +
                    "帮助用户看见自己的成长轨迹。语言要有温度、有洞察，不要泛泛而谈，直接输出寄语文字。\n\n" +
                    "已解锁勋章：" + String.join("、", unlocked) + "\n" +
                    "记忆总数：" + total + " 条，平均情感分值：" + String.format("%.2f", avg) + "（范围 -1 到 1，正数为积极）。";

            String insight = deepSeekClient.chat(null, prompt);
            return Result.success(insight);
        } catch (Exception e) {
            return Result.error("AI 寄语生成失败：" + e.getMessage());
        }
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0.0; }
    }
}
