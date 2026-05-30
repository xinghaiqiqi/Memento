package com.memento.aisubsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 心灵回声子系统专属 AI 配置。
 * 完全独立于源代码中的 ai.deepseek 配置（那是其他成员的资源）。
 * 配置前缀：my-ai
 */
@Component
@ConfigurationProperties(prefix = "my-ai")
public class MyAiProperties {

    /** 我自己的 DeepSeek API Key */
    private String apiKey;

    /** API 地址 */
    private String apiUrl = "https://api.deepseek.com/v1/chat/completions";

    /** 模型名称 */
    private String model = "deepseek-chat";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
