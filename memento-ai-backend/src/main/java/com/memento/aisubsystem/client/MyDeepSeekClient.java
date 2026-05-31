package com.memento.aisubsystem.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.memento.aisubsystem.config.MyAiProperties;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 心灵回声子系统专属的 DeepSeek 客户端。
 * 完全独立实现，不依赖源代码中的 AiUtils（那是其他成员的资源）。
 */
@Component
public class MyDeepSeekClient {

    @Autowired
    private MyAiProperties properties;

    /**
     * 是否已配置我自己的 API Key
     */
    public boolean isConfigured() {
        String key = properties.getApiKey();
        return key != null && !key.trim().isEmpty() && !key.startsWith("${");
    }

    /**
     * 通用对话调用。
     *
     * @param systemPrompt 系统人设提示（可为 null）
     * @param userMessage  用户消息
     * @return AI 回复文本；调用失败抛出 RuntimeException
     */
    public String chat(String systemPrompt, String userMessage) {
        if (!isConfigured()) {
            throw new IllegalStateException("尚未配置 my-ai.api-key，请设置你自己的 DeepSeek API Key");
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(properties.getApiUrl());
            httpPost.setHeader("Authorization", "Bearer " + properties.getApiKey());
            httpPost.setHeader("Content-Type", "application/json");

            JSONObject body = new JSONObject();
            body.put("model", properties.getModel());
            body.put("temperature", 0.7);

            JSONArray messages = new JSONArray();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JSONObject sys = new JSONObject();
                sys.put("role", "system");
                sys.put("content", systemPrompt);
                messages.add(sys);
            }
            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", userMessage);
            messages.add(user);
            body.put("messages", messages);

            StringEntity entity = new StringEntity(
                    body.toJSONString(), ContentType.APPLICATION_JSON,
                    StandardCharsets.UTF_8.name(), false);
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String respBody = new String(
                        response.getEntity().getContent().readAllBytes(),
                        StandardCharsets.UTF_8);
                return parseContent(respBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("DeepSeek 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 DeepSeek 返回体，提取回复内容，并清理可能的 markdown 代码块包裹。
     */
    private String parseContent(String respBody) {
        if (respBody == null || !respBody.trim().startsWith("{")) {
            throw new RuntimeException("DeepSeek 返回了非预期内容: " + respBody);
        }
        JSONObject json = JSON.parseObject(respBody);
        if (json.containsKey("error")) {
            JSONObject err = json.getJSONObject("error");
            String msg = err != null ? err.getString("message") : "未知错误";
            throw new RuntimeException("DeepSeek API 错误: " + msg);
        }
        String content = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
        return cleanMarkdown(content);
    }

    /**
     * 去除 ```json ... ``` 这类代码块包裹，便于后续 JSON 解析。
     */
    private String cleanMarkdown(String content) {
        if (content == null) {
            return "";
        }
        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }
}
