package com.memento.controller;

import com.alibaba.fastjson.JSON;
import com.memento.entity.ImportLog;
import com.memento.entity.Memory;
import com.memento.service.MemoryService;
import com.memento.util.AiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private AiUtils aiUtils;

    @PostMapping("/upload")
    public List<Memory> upload(@RequestParam("file") MultipartFile file) throws Exception {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return extract(content);
    }

    @PostMapping("/text")
    public List<Memory> uploadText(@RequestBody Map<String, String> body) {
        String content = body.get("text");
        return extract(content);
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
            memoryService.saveMemory(m);
        }
        return true;
    }
}
