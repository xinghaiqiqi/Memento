package com.memento.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memento.entity.Milestone;
import com.memento.mapper.MilestoneMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

    @Autowired
    private MilestoneMapper milestoneMapper;

    @GetMapping
    public List<Milestone> list() {
        return milestoneMapper.selectList(
                new LambdaQueryWrapper<Milestone>().eq(Milestone::getUserId, 1L).orderByDesc(Milestone::getEventDate)
        );
    }

    @PostMapping
    public boolean save(@RequestBody Milestone milestone) {
        if (milestone.getUserId() == null) milestone.setUserId(1L);
        return milestoneMapper.insert(milestone) > 0;
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return milestoneMapper.deleteById(id) > 0;
    }
}
