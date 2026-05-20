package com.memento.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memento.dto.MilestoneDTO;
import com.memento.entity.Memory;
import com.memento.entity.Milestone;
import com.memento.mapper.MilestoneMapper;
import com.memento.service.MemoryService;
import com.memento.service.MilestoneService;
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
public class MilestoneServiceImpl extends ServiceImpl<MilestoneMapper, Milestone> implements MilestoneService {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private AiUtils aiUtils;

    @Override
    public List<MilestoneDTO> getAllMilestones(Long userId) {
        List<Milestone> milestones = list(new LambdaQueryWrapper<Milestone>()
                .eq(Milestone::getUserId, userId)
                .orderByDesc(Milestone::getEventDate));
        
        return milestones.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public MilestoneDTO getMilestoneById(Long id) {
        Milestone milestone = getById(id);
        return milestone != null ? convertToDTO(milestone) : null;
    }

    @Override
    @Transactional
    public List<MilestoneDTO> identifyMilestones(Long userId) {
        List<Memory> memories = memoryService.list(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getIsDeleted, false)
                .orderByDesc(Memory::getEventDate)
                .last("limit 100"));
        
        if (memories.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Milestone> newMilestones = new ArrayList<>();
        
        for (Memory memory : memories) {
            String nodeType = aiUtils.identifyMilestone(memory.getContent());
            
            if (!"NONE".equals(nodeType)) {
                Milestone milestone = new Milestone();
                milestone.setUserId(userId);
                milestone.setMemoryId(memory.getId());
                milestone.setNodeType(nodeType);
                milestone.setCustomTitle(null);
                milestone.setEventDate(memory.getEventDate());
                milestone.setIsAuto(true);
                milestone.setCreatedAt(LocalDateTime.now());
                
                newMilestones.add(milestone);
            }
        }
        
        if (!newMilestones.isEmpty()) {
            saveBatch(newMilestones);
        }
        
        return newMilestones.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MilestoneDTO addCustomMilestone(Milestone milestone) {
        if (milestone.getUserId() == null) {
            milestone.setUserId(1L);
        }
        milestone.setIsAuto(false);
        milestone.setCreatedAt(LocalDateTime.now());
        
        save(milestone);
        return convertToDTO(milestone);
    }

    @Override
    @Transactional
    public boolean deleteMilestone(Long id) {
        return removeById(id);
    }

    private MilestoneDTO convertToDTO(Milestone milestone) {
        MilestoneDTO dto = new MilestoneDTO();
        BeanUtils.copyProperties(milestone, dto);
        
        Memory memory = memoryService.getById(milestone.getMemoryId());
        if (memory != null) {
            MilestoneDTO.MemoryDetail memoryDetail = new MilestoneDTO.MemoryDetail();
            memoryDetail.setId(memory.getId());
            memoryDetail.setTitle(memory.getTitle());
            memoryDetail.setContent(memory.getContent());
            memoryDetail.setEventDate(memory.getEventDate());
            memoryDetail.setSentimentScore(memory.getSentimentScore());
            dto.setMemory(memoryDetail);
        }
        
        return dto;
    }
}