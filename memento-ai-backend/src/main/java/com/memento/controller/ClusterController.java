package com.memento.controller;

import com.memento.dto.Result;
import com.memento.dto.TopicClusterDTO;
import com.memento.service.ClusterService;
import com.memento.util.SecurityUtils;
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
        return Result.success(clusterService.getAllClusters(SecurityUtils.getCurrentUserId()));
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
    public Result<List<TopicClusterDTO>> run() {
        return Result.success(clusterService.runClustering(SecurityUtils.getCurrentUserId()));
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