package com.memento.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.memento.entity.Memory;

public interface MemoryService extends IService<Memory> {
    boolean saveMemory(Memory memory);
    boolean updateMemory(Memory memory);
    void analyzeSentiment(Long id);
}
