package com.memento.controller;

import com.memento.dto.Result;
import com.memento.dto.TopicClusterDTO;
import com.memento.service.ClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clusters")
public class ClusterController {

    @Autowired
    private ClusterService clusterService;

    @GetMapping
    public Result<List<TopicClusterDTO>> list() {
        List<TopicClusterDTO> clusters = clusterService.getAllClusters(1L);
        return Result.success(clusters);
    }

    @GetMapping("/{id}")
    public Result<TopicClusterDTO> getById(@PathVariable Long id) {
        TopicClusterDTO cluster = clusterService.getClusterById(id);
        return cluster != null ? Result.success(cluster) : Result.error("主题不存在");
    }

    @GetMapping("/{id}/memories")
    public Result<TopicClusterDTO> getClusterMemories(@PathVariable Long id) {
        TopicClusterDTO cluster = clusterService.getClusterMemories(id);
        return cluster != null ? Result.success(cluster) : Result.error("主题不存在");
    }

    @PostMapping("/run")
    public Result<TopicClusterDTO> runClustering() {
        try {
            TopicClusterDTO cluster = clusterService.runClustering(1L);
            return Result.success("聚类完成", cluster);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/rename")
    public Result<Boolean> rename(@PathVariable Long id, @RequestParam String name) {
        boolean success = clusterService.renameCluster(id, name);
        return success ? Result.success("重命名成功", true) : Result.error("重命名失败");
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = clusterService.deleteCluster(id);
        return success ? Result.success("删除成功", true) : Result.error("删除失败");
    }
}