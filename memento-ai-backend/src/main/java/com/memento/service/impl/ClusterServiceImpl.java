package com.memento.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memento.dto.TopicClusterDTO;
import com.memento.entity.Memory;
import com.memento.entity.TopicCluster;
import com.memento.mapper.TopicClusterMapper;
import com.memento.service.ClusterService;
import com.memento.service.MemoryService;
import com.memento.util.AiUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClusterServiceImpl extends ServiceImpl<TopicClusterMapper, TopicCluster> implements ClusterService {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private AiUtils aiUtils;

    @Override
    public List<TopicClusterDTO> getAllClusters(Long userId) {
        List<TopicCluster> clusters = list(new LambdaQueryWrapper<TopicCluster>()
                .eq(TopicCluster::getUserId, userId)
                .orderByDesc(TopicCluster::getUpdatedAt));
        
        return clusters.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public TopicClusterDTO getClusterById(Long id) {
        TopicCluster cluster = getById(id);
        return cluster != null ? convertToDTO(cluster) : null;
    }

    @Override
    public TopicClusterDTO getClusterMemories(Long clusterId) {
        TopicCluster cluster = getById(clusterId);
        if (cluster == null) return null;
        
        TopicClusterDTO dto = convertToDTO(cluster);
        
        if (cluster.getMemoryIds() != null && !cluster.getMemoryIds().isEmpty()) {
            List<Long> memoryIds = JSON.parseArray(cluster.getMemoryIds(), Long.class);
            List<Memory> memories = memoryService.listByIds(memoryIds);
            
            dto.setMemories(memories.stream().map(m -> {
                TopicClusterDTO.MemorySummary summary = new TopicClusterDTO.MemorySummary();
                summary.setId(m.getId());
                summary.setTitle(m.getTitle());
                summary.setContent(m.getContent());
                summary.setEventDate(m.getEventDate().toString());
                summary.setSentimentScore(m.getSentimentScore() != null ? m.getSentimentScore().doubleValue() : 0.0);
                summary.setPhotoUrl(m.getPhotoUrl()); // 添加照片 URL
                return summary;
            }).collect(Collectors.toList()));
            
            dto.setMemoryCount(memories.size());
        }
        
        return dto;
    }

    @Override
    @Transactional
    public List<TopicClusterDTO> runClustering(Long userId) {
        List<Memory> memories = memoryService.list(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsDeleted, false)
                .orderByDesc(Memory::getEventDate)
                .last("limit 50"));
        
        if (memories.isEmpty()) {
            throw new RuntimeException("没有足够的记忆进行聚类");
        }
        
        String text = memories.stream()
                .map(m -> "[" + m.getId() + "] " + m.getTitle() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
        
        String aiResult = aiUtils.generateClusters(text);
        
        JSONObject result = JSON.parseObject(aiResult);
        JSONArray clusters = result.getJSONArray("clusters");
        
        // 只有在成功获取并解析 AI 结果后，才清理旧的聚类数据
        remove(new LambdaQueryWrapper<TopicCluster>().eq(TopicCluster::getUserId, userId));
        
        List<TopicCluster> newClusters = new ArrayList<>();
        
        for (int i = 0; i < clusters.size(); i++) {
            JSONObject clusterJson = clusters.getJSONObject(i);
            TopicCluster cluster = new TopicCluster();
            cluster.setUserId(userId);
            cluster.setName(clusterJson.getString("name"));
            cluster.setDescription(clusterJson.getString("description"));
            
            JSONArray memoryIdArray = clusterJson.getJSONArray("memoryIds");
            if (memoryIdArray != null) {
                cluster.setMemoryIds(memoryIdArray.toJSONString());
            }
            
            cluster.setCreatedAt(LocalDateTime.now());
            cluster.setUpdatedAt(LocalDateTime.now());
            
            newClusters.add(cluster);
        }
        
        saveBatch(newClusters);
        
        return newClusters.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean renameCluster(Long clusterId, String newName) {
        TopicCluster cluster = getById(clusterId);
        if (cluster == null) return false;
        
        cluster.setName(newName);
        cluster.setUpdatedAt(LocalDateTime.now());
        return updateById(cluster);
    }

    @Override
    @Transactional
    public boolean deleteCluster(Long clusterId) {
        return removeById(clusterId);
    }

    private TopicClusterDTO convertToDTO(TopicCluster cluster) {
        TopicClusterDTO dto = new TopicClusterDTO();
        BeanUtils.copyProperties(cluster, dto);
        
        if (cluster.getMemoryIds() != null && !cluster.getMemoryIds().isEmpty()) {
            List<Long> memoryIds = JSON.parseArray(cluster.getMemoryIds(), Long.class);
            dto.setMemoryIds(memoryIds);
            dto.setMemoryCount(memoryIds.size());
        } else {
            dto.setMemoryCount(0);
        }
        
        return dto;
    }
}