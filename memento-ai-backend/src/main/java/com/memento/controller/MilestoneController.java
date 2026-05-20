package com.memento.controller;

import com.memento.dto.MilestoneDTO;
import com.memento.dto.Result;
import com.memento.entity.Milestone;
import com.memento.service.MilestoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

    @Autowired
    private MilestoneService milestoneService;

    @GetMapping
    public Result<List<MilestoneDTO>> list() {
        List<MilestoneDTO> milestones = milestoneService.getAllMilestones(1L);
        return Result.success(milestones);
    }

    @GetMapping("/{id}")
    public Result<MilestoneDTO> getById(@PathVariable Long id) {
        MilestoneDTO milestone = milestoneService.getMilestoneById(id);
        return milestone != null ? Result.success(milestone) : Result.error("节点不存在");
    }

    @PostMapping("/identify")
    public Result<List<MilestoneDTO>> identify() {
        try {
            List<MilestoneDTO> milestones = milestoneService.identifyMilestones(1L);
            return Result.success("识别完成，发现 " + milestones.size() + " 个新节点", milestones);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<MilestoneDTO> add(@RequestBody Milestone milestone) {
        try {
            MilestoneDTO dto = milestoneService.addCustomMilestone(milestone);
            return Result.success("添加成功", dto);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = milestoneService.deleteMilestone(id);
        return success ? Result.success("删除成功", true) : Result.error("删除失败");
    }
}