package com.memento.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class AiUtils {

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    @Value("${ai.deepseek.api-url}")
    private String apiUrl;

    @Value("${ai.deepseek.model}")
    private String model;

    /**
     * 调用大模型对话接口
     */
    public String chat(String prompt) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setHeader("Content-Type", "application/json");

            JSONObject body = new JSONObject();
            body.put("model", model);
            
            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            
            body.put("messages", messages);
            body.put("temperature", 0.7);

            StringEntity entity = new StringEntity(body.toJSONString(), ContentType.APPLICATION_JSON, StandardCharsets.UTF_8.name(), false);
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                
                // 打印响应体，方便调试
                System.out.println("AI Response: " + responseBody);

                JSONObject jsonResponse = JSON.parseObject(responseBody);
                
                // 检查 API 返回的错误
                if (jsonResponse.containsKey("error")) {
                    JSONObject error = jsonResponse.getJSONObject("error");
                    return "AI API Error: " + error.getString("message");
                }

                String content = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                
                // 自动去除 Markdown 代码块包裹
                if (content != null) {
                    content = content.trim();
                    if (content.startsWith("```json")) {
                        content = content.substring(7);
                    } else if (content.startsWith("```")) {
                        content = content.substring(3);
                    }
                    if (content.endsWith("```")) {
                        content = content.substring(0, content.length() - 3);
                    }
                }
                return content != null ? content.trim() : "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "AI调用失败: " + e.getMessage();
        }
    }

    /**
     * 获取情感分数 (-1 到 1)
     */
    public Double analyzeSentiment(String content) {
        String prompt = String.format("分析以下文本的情感倾向，只返回一个-1到1之间的数字，不要有任何其他内容。\n文本：%s", content);
        String result = chat(prompt).trim();
        try {
            return Double.parseDouble(result);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 生成主题聚类
     */
    public String generateClusters(String memoriesText) {
        String prompt = String.format(
            "以下是用户的记忆片段列表（每行一条）：\n%s\n" +
            "请分析出3-5个核心主题，每个主题包含：主题名称（2-6个字）、主题描述（一句话）、包含的记忆内容摘要。\n" +
            "请以简洁的JSON格式返回，不要有任何 Markdown 代码块包裹或额外解释。", 
            memoriesText
        );
        return chat(prompt);
    }

    /**
     * 识别重要节点
     */
    public String identifyMilestone(String content) {
        String prompt = String.format(
            "判断以下记忆是否属于人生重要节点（如毕业、入职、搬家、分手、考试通过、旅行等）。\n" +
            "记忆：%s\n" +
            "只返回节点类型（如：毕业），如果不属于则返回 NONE。", 
            content
        );
        return chat(prompt).trim();
    }

    /**
     * 从原始聊天记录中提取重要记忆事件
     */
    public String extractMemoriesFromChat(String rawText) {
        String prompt = String.format(
            "你是一个记忆提取专家。以下是一段原始的聊天记录或日志内容：\n" +
            "------\n%s\n------\n" +
            "请从中分析并提取出具有纪念意义的‘重要事件’。提取规则：\n" +
            "1. 忽略琐碎的日常问候和无意义对话。\n" +
            "2. 每个事件必须包含：标题（简洁）、内容描述（详细一点）、大概发生日期（YYYY-MM-DD格式，如果内容中没提到具体年份请推测或使用2024）。\n" +
            "3. 请以JSON数组格式返回，例如：[{\"title\":\"...\", \"content\":\"...\", \"eventDate\":\"...\"}]。\n" +
            "不要包含任何解释性文字，只返回JSON数组。", 
            rawText
        );
        return chat(prompt);
    }

    /**
     * 生成叙事
     */
    public String generateNarrative(String memoriesList, String style) {
        String prompt = String.format(
            "你是一位回忆录作家。以下是我的记忆片段（按时间排序）：\n%s\n" +
            "请用第一人称写一段叙事，风格为%s，将这些记忆串联成一个完整的故事。要求有情感起伏、细节描写、自然的过渡。", 
            memoriesList, style
        );
        return chat(prompt);
    }
}
