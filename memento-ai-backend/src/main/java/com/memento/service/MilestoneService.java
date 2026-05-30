package com.memento.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.memento.dto.MilestoneDTO;
import com.memento.entity.Milestone;

import java.util.List;

public interface MilestoneService extends IService<Milestone> {
    List<MilestoneDTO> getAllMilestones(Long userId);
    MilestoneDTO getMilestoneById(Long id);
    List<MilestoneDTO> identifyMilestones(Long userId);
    MilestoneDTO addCustomMilestone(Milestone milestone);
    boolean deleteMilestone(Long id);
}