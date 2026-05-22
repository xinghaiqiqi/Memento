package com.memento.controller;

import com.alibaba.fastjson.JSON;
import com.memento.entity.Memory;
import com.memento.service.MemoryService;
import com.memento.util.AiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private AiUtils aiUtils;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/upload")
    public List<Memory> upload(@RequestParam("file") MultipartFile file) throws Exception {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return extract(content);
    }

    @PostMapping("/text")
    public List<Memory> importText(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        System.out.println("\n>>> [Controller] Received text import request. Content: " + (text.length() > 20 ? text.substring(0, 20) + "..." : text));
        String jsonResult = aiUtils.extractMemoriesFromChat(text);
        System.out.println(">>> [Controller] AI Extraction Result: " + jsonResult);
        
        try {
            List<Memory> memories = JSON.parseArray(jsonResult, Memory.class);
            System.out.println(">>> [Controller] Successfully parsed " + memories.size() + " memories.");
            return memories;
        } catch (Exception e) {
            System.err.println("!!! [Controller] Failed to parse AI result to Memory list: " + e.getMessage());
            return List.of();
        }
    }

    private List<Memory> extract(String rawText) {
        String jsonResult = aiUtils.extractMemoriesFromChat(rawText);
        try {
            // 清理可能存在的 Markdown 包裹
            if (jsonResult.contains("```json")) {
                jsonResult = jsonResult.substring(jsonResult.indexOf("```json") + 7, jsonResult.lastIndexOf("```"));
            } else if (jsonResult.contains("```")) {
                jsonResult = jsonResult.substring(jsonResult.indexOf("```") + 3, jsonResult.lastIndexOf("```"));
            }
            return JSON.parseArray(jsonResult.trim(), Memory.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @PostMapping("/confirm")
    public boolean confirm(@RequestBody List<Memory> memories) {
        for (Memory m : memories) {
            // 生成记忆内容的唯一标识 (基于内容和用户 ID)
            String contentHash = cn.hutool.crypto.digest.DigestUtil.md5Hex(m.getContent().trim());
            String duplicateKey = "memory:dup:" + (m.getUserId() != null ? m.getUserId() : 1) + ":" + contentHash;

            // 检查 Redis 中是否存在该记忆
            if (Boolean.TRUE.equals(redisTemplate.hasKey(duplicateKey))) {
                // 双重检查：如果 Redis 认为重复，但数据库其实是空的（比如重置了 DB），则允许插入
                long count = memoryService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Memory>()
                        .eq(Memory::getContent, m.getContent().trim())
                        .eq(Memory::getIsDeleted, false));
                
                if (count > 0) {
                    System.out.println(">>> [Deduplication] Skip true duplicate: " + m.getTitle());
                    continue;
                } else {
                    System.out.println(">>> [Deduplication] Redis hit but DB empty, allowing re-insert.");
                }
            }

            // 保存记忆
            memoryService.saveMemory(m);

            // 记录到 Redis，防止 1 小时内频繁重复点击
            redisTemplate.opsForValue().set(duplicateKey, "1", 1, TimeUnit.HOURS);
        }
        return true;
    }
}
