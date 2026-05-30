package com.memento.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memento.entity.Memory;
import com.memento.mapper.MemoryMapper;
import com.memento.service.MemoryService;
import com.memento.util.AiUtils;
import com.memento.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class MemoryServiceImpl extends ServiceImpl<MemoryMapper, Memory> implements MemoryService {

    @Autowired
    private AiUtils aiUtils;

    @Override
    public boolean saveMemory(Memory memory) {
        if (memory.getUserId() == null) {
            memory.setUserId(SecurityUtils.getCurrentUserId()); // 默认从上下文获取
        }
        boolean saved = save(memory);
        if (saved) {
            analyzeSentiment(memory.getId());
        }
        return saved;
    }

    @Override
    public boolean updateMemory(Memory memory) {
        boolean updated = updateById(memory);
        if (updated) {
            analyzeSentiment(memory.getId());
        }
        return updated;
    }

    @Override
    @Async
    public void analyzeSentiment(Long id) {
        Memory memory = getById(id);
        if (memory != null) {
            Double score = aiUtils.analyzeSentiment(memory.getContent());
            memory.setSentimentScore(BigDecimal.valueOf(score));
            memory.setSentimentAnalyzedAt(LocalDateTime.now());
            updateById(memory);
        }
    }
}
