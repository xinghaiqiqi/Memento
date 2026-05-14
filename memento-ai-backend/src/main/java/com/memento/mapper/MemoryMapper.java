package com.memento.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memento.entity.Memory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemoryMapper extends BaseMapper<Memory> {
}
