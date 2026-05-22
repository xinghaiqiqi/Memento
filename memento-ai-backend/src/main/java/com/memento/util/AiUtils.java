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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class AiUtils {

    @Autowired
    private StringRedisTemplate redisTemplate;

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
        // 清理 prompt，去除首尾空格并统一换行符，提高缓存命中率
        String cleanPrompt = prompt.trim().replace("\r\n", "\n");
        String cacheKey = "ai:cache:" + cn.hutool.crypto.digest.DigestUtil.md5Hex(cleanPrompt);
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            System.out.println(">>> [Redis] Cache Hit for AI Request.");
            return cachedValue;
        }

        System.out.println("\n--- [DeepSeek API Request] ---");
        System.out.println("Prompt: " + cleanPrompt);

        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("${")) {
            System.out.println(">>> System Notice: API Key missing, using Demo Mode.");
            return getDemoResponse(cleanPrompt, "unknown");
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Authorization", "Bearer " + apiKey);
            httpPost.setHeader("Content-Type", "application/json");

            JSONObject body = new JSONObject();
            body.put("model", model);
            
            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", cleanPrompt);
            messages.add(message);
            
            body.put("messages", messages);
            body.put("temperature", 0.7);

            StringEntity entity = new StringEntity(body.toJSONString(), ContentType.APPLICATION_JSON, StandardCharsets.UTF_8.name(), false);
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("--- [DeepSeek API Response] ---");
                
                if (responseBody == null || !responseBody.trim().startsWith("{")) {
                    System.err.println("!!! [DeepSeek API Error]: Invalid JSON response. Body: " + responseBody);
                    return getDemoResponse(cleanPrompt, "unknown");
                }

                JSONObject jsonResponse = JSON.parseObject(responseBody);
                
                if (jsonResponse.containsKey("error")) {
                    JSONObject errorObj = jsonResponse.getJSONObject("error");
                    String errorMsg = errorObj != null ? errorObj.getString("message") : "Unknown Error";
                    System.err.println("!!! [DeepSeek API Error]: " + errorMsg);
                    return getDemoResponse(cleanPrompt, "unknown");
                }

                String content = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                
                String processed = processContent(content);
                System.out.println("Processed Content: " + processed);

                // 写入缓存 (有效期 24 小时)
                redisTemplate.opsForValue().set(cacheKey, processed, 24, TimeUnit.HOURS);
                System.out.println(">>> [Redis] Cache Saved for prompt.");
                
                return processed;
            }
        } catch (Exception e) {
            System.err.println("!!! [DeepSeek Connection Exception]: " + e.getMessage());
            return getDemoResponse(cleanPrompt, "unknown");
        }
    }

    private String processContent(String content) {
        if (content != null) {
            content = content.trim();
            if (content.startsWith("```json")) content = content.substring(7);
            else if (content.startsWith("```")) content = content.substring(3);
            if (content.endsWith("```")) content = content.substring(0, content.length() - 3);
            return content.trim();
        }
        return "";
    }

    private String getDemoResponse(String prompt, String type) {
        if (prompt.contains("倾向") || "sentiment".equals(type)) {
            return String.valueOf(Math.random() * 2 - 1);
        }
        if (prompt.contains("核心主题") || "clusters".equals(type)) {
            return "[{\"name\":\"艺术生活\",\"description\":\"关于美与创造的瞬间\",\"memoryIds\":\"[]\"}]";
        }
        if (prompt.contains("提取") || "extract".equals(type)) {
            return "[{\"title\":\"示例记忆\",\"content\":\"这是一条演示数据。\",\"eventDate\":\"2024-05-20\"}]";
        }
        return "[]";
    }

    public Double analyzeSentiment(String content) {
        System.out.println("\n--- [Sentiment Analysis] ---");
        String prompt = String.format("分析以下文本的情感倾向，只返回一个-1到1之间的数字：\n%s", content);
        String result = chat(prompt);
        try {
            return Double.parseDouble(result);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public String generateClusters(String memoriesText) {
        System.out.println("\n--- [Topic Clustering] ---");
        String prompt = String.format("分析以下记忆片段的主题聚类，以JSON格式返回：\n%s", memoriesText);
        return chat(prompt);
    }

    public String identifyMilestone(String content) {
        System.out.println("\n--- [Milestone Identification] ---");
        String prompt = String.format("判断以下记忆是否属于重要节点，只返回节点类型或 NONE：\n%s", content);
        return chat(prompt);
    }

    public String extractMemoriesFromChat(String rawText) {
        System.out.println("\n--- [Memory Extraction] ---");
        String today = java.time.LocalDate.now().toString();
        String prompt = String.format(
            "你是一个记忆提取专家。请从以下文本中提取重要事件。提取规则：\n" +
            "1. 严格时间基准：当前的完整日期是 %s（请务必使用此日期作为年份、月份和日期的计算起点）。\n" +
            "2. 相对时间换算：如果文本提到‘今天’，日期必须是 %s；如果提到‘昨天’，请推算为 %s 的前一天；如果未提及年份，默认必须使用 %s 年。\n" +
            "3. 每个事件必须包含：\n" +
            "   - title（标题）\n" +
            "   - content（描述）\n" +
            "   - eventDate（格式：YYYY-MM-DD）\n" +
            "   - sentimentScore（情感分值，范围 -1.0 到 1.0，积极为正，消极为负）\n" +
            "4. 仅返回 JSON 数组，禁止任何解释性文字。\n" +
            "5. 格式参考：[{\"title\":\"...\", \"content\":\"...\", \"eventDate\":\"%s\", \"sentimentScore\": 0.8}]\n\n" +
            "待处理文本内容：\n%s", 
            today, today, today, today.substring(0, 4), today, rawText
        );
        return chat(prompt);
    }

    public String generateNarrative(String memoriesList, String style) {
        System.out.println("\n--- [Narrative Generation] ---");
        String prompt = String.format("请用%s风格将以下记忆串联成故事：\n%s", style, memoriesList);
        return chat(prompt);
    }
}
