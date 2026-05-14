package com.memento.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memento.entity.Memory;
import com.memento.entity.TopicCluster;
import com.memento.mapper.TopicClusterMapper;
import com.memento.service.MemoryService;
import com.memento.util.AiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clusters")
public class ClusterController {

    @Autowired
    private TopicClusterMapper topicClusterMapper;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private AiUtils aiUtils;

    @GetMapping
    public List<TopicCluster> list() {
        return topicClusterMapper.selectList(
                new LambdaQueryWrapper<TopicCluster>().eq(TopicCluster::getUserId, 1L)
        );
    }

    @PostMapping("/run")
    public String run() {
        List<Memory> memories = memoryService.list(
                new LambdaQueryWrapper<Memory>().eq(Memory::getUserId, 1L).orderByDesc(Memory::getEventDate).last("limit 20")
        );
        
        if (memories.isEmpty()) {
            return "{\"error\": \"没有足够的记忆进行聚类\"}";
        }
        
        String text = memories.stream().map(m -> m.getTitle() + ": " + m.getContent()).collect(Collectors.joining("\n"));
        return aiUtils.generateClusters(text);
    }
}
