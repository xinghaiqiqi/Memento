package com.memento.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memento.dto.Result;
import com.memento.entity.GeneratedNarrative;
import com.memento.entity.Memory;
import com.memento.entity.TopicCluster;
import com.memento.entity.User;
import com.memento.mapper.GeneratedNarrativeMapper;
import com.memento.mapper.TopicClusterMapper;
import com.memento.mapper.UserMapper;
import com.memento.service.MemoryService;
import com.memento.util.PdfUtils;
import com.memento.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private TopicClusterMapper topicClusterMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GeneratedNarrativeMapper generatedNarrativeMapper;

    @PostMapping("/create")
    public ResponseEntity<byte[]> createAndDownload(@RequestBody Map<String, Object> params) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        try {
            String layout = (String) params.getOrDefault("layout", "classic");
            if (!"classic".equals(layout) && !"modern".equals(layout)) {
                return exportError("无法导出：排版风格配置错误，请选择经典书信或现代简约。", HttpStatus.BAD_REQUEST);
            }

            List<?> includes = params.get("includes") instanceof List<?> includeList
                    ? includeList : List.of("memories");
            boolean includeMemories = includes.contains("memories");
            boolean includeNarratives = includes.contains("narratives");
            boolean includeCharts = includes.contains("charts");
            if (!includeMemories && !includeNarratives && !includeCharts) {
                return exportError("无法导出：请至少选择一项需要导出的内容。", HttpStatus.BAD_REQUEST);
            }

            // 查询用户的记忆数据，目录和正文按时间从早到晚排列
            LambdaQueryWrapper<Memory> query = new LambdaQueryWrapper<>();
            query.eq(Memory::getUserId, userId)
                 .eq(Memory::getIsDeleted, false)
                 .orderByAsc(Memory::getEventDate)
                 .orderByAsc(Memory::getId);

            // 处理日期范围过滤
            String range = (String) params.getOrDefault("range", "all");
            if ("date".equals(range)) {
                Object dateRangeValue = params.get("dateRange");
                if (!(dateRangeValue instanceof List<?> dateRange) || dateRange.size() != 2
                        || dateRange.get(0) == null || dateRange.get(1) == null) {
                    return exportError("无法导出：请选择完整的开始日期和结束日期。", HttpStatus.BAD_REQUEST);
                }

                LocalDate startDate = LocalDate.parse(String.valueOf(dateRange.get(0)));
                LocalDate endDate = LocalDate.parse(String.valueOf(dateRange.get(1)));
                if (startDate.isAfter(endDate)) {
                    return exportError("无法导出：开始日期不能晚于结束日期。", HttpStatus.BAD_REQUEST);
                }
                query.ge(Memory::getEventDate, startDate)
                     .le(Memory::getEventDate, endDate);
            } else if ("cluster".equals(range)) {
                Object selectedClustersValue = params.get("selectedClusters");
                if (!(selectedClustersValue instanceof List<?> selectedClusters) || selectedClusters.isEmpty()) {
                    return exportError("无法导出：请选择至少一个主题。", HttpStatus.BAD_REQUEST);
                }

                Set<Long> memoryIds = resolveClusterMemoryIds(userId, selectedClusters);
                if (memoryIds.isEmpty()) {
                    return exportError("无法导出：所选主题不存在，或主题下没有关联记忆。", HttpStatus.BAD_REQUEST);
                }
                query.in(Memory::getId, memoryIds);
            } else if (!"all".equals(range)) {
                return exportError("无法导出：导出范围配置错误。", HttpStatus.BAD_REQUEST);
            }

            List<Memory> scopedMemories = memoryService.list(query);
            List<Memory> memories = includeMemories ? scopedMemories : new ArrayList<>();
            List<Memory> chartMemories = includeCharts ? scopedMemories : new ArrayList<>();
            List<GeneratedNarrative> narratives = includeNarratives ? loadNarratives(userId) : new ArrayList<>();

            if (includeMemories && memories.isEmpty()) {
                return exportError("无法导出：当前导出范围内没有可导出的原始记忆。", HttpStatus.BAD_REQUEST);
            }
            if (includeNarratives && narratives.isEmpty()) {
                return exportError("无法导出：你选择了“AI叙事”，但当前还没有已保存的 AI 叙事内容。请先到“博物馆概览-编织叙事”页面生成并保存内容，或取消勾选“AI叙事”。", HttpStatus.BAD_REQUEST);
            }
            if (includeCharts && chartMemories.isEmpty()) {
                return exportError("无法导出：你选择了“统计图表”，但当前导出范围内没有可统计的记忆数据。", HttpStatus.BAD_REQUEST);
            }
            if (memories.isEmpty() && narratives.isEmpty() && chartMemories.isEmpty()) {
                return exportError("无法导出：当前配置下没有可写入 PDF 的内容。", HttpStatus.BAD_REQUEST);
            }

            // 生成PDF
            String title = "拾光记";
            String author = getCurrentUserNickname(userId);
            byte[] pdfBytes = "modern".equals(layout)
                    ? PdfUtils.generateModernTextMemoryBook(memories, narratives, chartMemories, title, author)
                    : PdfUtils.generateMemoryBook(memories, narratives, chartMemories, title, author);

            // 设置响应头
            String filename = "memento_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (DateTimeParseException e) {
            return exportError("无法导出：日期格式不正确，请重新选择日期范围。", HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            e.printStackTrace();
            return exportError("无法导出：PDF 生成失败，请稍后重试。", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getCurrentUserNickname(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return "记忆收藏家";
    }

    private Set<Long> resolveClusterMemoryIds(Long userId, List<?> selectedClusters) {
        Set<Long> memoryIds = new LinkedHashSet<>();
        for (Object clusterIdValue : selectedClusters) {
            if (clusterIdValue == null) {
                continue;
            }
            try {
                Long clusterId = Long.valueOf(String.valueOf(clusterIdValue));
                TopicCluster cluster = topicClusterMapper.selectOne(new LambdaQueryWrapper<TopicCluster>()
                        .eq(TopicCluster::getUserId, userId)
                        .eq(TopicCluster::getId, clusterId));
                if (cluster == null || cluster.getMemoryIds() == null || cluster.getMemoryIds().isBlank()) {
                    continue;
                }
                for (String idText : cluster.getMemoryIds().split(",")) {
                    String trimmed = idText.trim();
                    if (!trimmed.isEmpty()) {
                        memoryIds.add(Long.valueOf(trimmed));
                    }
                }
            } catch (NumberFormatException ignored) {
                // 忽略格式错误的主题或记忆 ID，最终由空结果提示具体原因
            }
        }
        return memoryIds;
    }

    private List<GeneratedNarrative> loadNarratives(Long userId) {
        return generatedNarrativeMapper.selectList(new LambdaQueryWrapper<GeneratedNarrative>()
                .eq(GeneratedNarrative::getUserId, userId)
                .orderByAsc(GeneratedNarrative::getCreatedAt)
                .orderByAsc(GeneratedNarrative::getId));
    }

    private ResponseEntity<byte[]> exportError(String message, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return ResponseEntity.status(status)
                .headers(headers)
                .body(("{\"code\":500,\"message\":\"" + escapeJson(message) + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @GetMapping("/preview")
    public Result<List<Memory>> previewMemories() {
        Long userId = SecurityUtils.getCurrentUserId();
        
        List<Memory> memories = memoryService.list(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsDeleted, false)
                .orderByDesc(Memory::getEventDate)
                .last("limit 5"));

        return Result.success(memories);
    }

    @GetMapping("/test-pdf")
    public ResponseEntity<byte[]> testPdf() {
        try {
            // 创建模拟数据
            Memory testMemory = new Memory();
            testMemory.setId(1L);
            testMemory.setTitle("人生第一辆车");
            testMemory.setContent("买了人生第一辆车，心情非常激动！\n\n我终于有了属于自己的车，可以自由地到处游玩了。");
            testMemory.setEventDate(LocalDate.now());
            testMemory.setTags("生活, 激动");
            testMemory.setSentimentScore(java.math.BigDecimal.valueOf(0.9));

            java.util.List<Memory> memories = java.util.Arrays.asList(testMemory);

            // 生成PDF
            byte[] pdfBytes = PdfUtils.generateMemoryBook(memories, "测试标题", "测试作者");

            // 设置响应头
            String filename = "test_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}