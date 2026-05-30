package com.memento.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.memento.dto.TopicClusterDTO;
import com.memento.entity.TopicCluster;

import java.util.List;

public interface ClusterService extends IService<TopicCluster> {
    List<TopicClusterDTO> getAllClusters(Long userId);
    TopicClusterDTO getClusterById(Long id);
    TopicClusterDTO getClusterMemories(Long clusterId);
    List<TopicClusterDTO> runClustering(Long userId);
    boolean renameCluster(Long clusterId, String newName);
    boolean deleteCluster(Long clusterId);
}