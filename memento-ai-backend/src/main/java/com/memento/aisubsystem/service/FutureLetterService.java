package com.memento.aisubsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.memento.aisubsystem.entity.FutureLetter;

import java.util.List;

/**
 * 未来邮局服务接口（心灵回声子系统专属）
 */
public interface FutureLetterService extends IService<FutureLetter> {

    /**
     * 获取当前用户所有信件，并动态计算 status
     */
    List<FutureLetter> listByUser(Long userId);

    /**
     * 保存一封信件
     */
    FutureLetter saveLetter(FutureLetter letter);

    /**
     * 删除一封信件
     */
    boolean removeLetter(Long id, Long userId);
}
