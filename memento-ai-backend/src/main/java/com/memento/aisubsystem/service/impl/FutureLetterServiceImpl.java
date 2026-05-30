package com.memento.aisubsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memento.aisubsystem.entity.FutureLetter;
import com.memento.aisubsystem.mapper.FutureLetterMapper;
import com.memento.aisubsystem.service.FutureLetterService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 未来邮局服务实现（心灵回声子系统专属）
 */
@Service
public class FutureLetterServiceImpl
        extends ServiceImpl<FutureLetterMapper, FutureLetter>
        implements FutureLetterService {

    @Override
    public List<FutureLetter> listByUser(Long userId) {
        List<FutureLetter> letters = list(
                new LambdaQueryWrapper<FutureLetter>()
                        .eq(FutureLetter::getUserId, userId)
                        .orderByAsc(FutureLetter::getTargetDate)
        );
        // 动态计算 status：不依赖数据库存储的值，每次查询时实时判断
        LocalDateTime now = LocalDateTime.now();
        letters.forEach(l -> l.setStatus(
                now.isAfter(l.getTargetDate()) ? "unlocked" : "locked"
        ));
        return letters;
    }

    @Override
    public FutureLetter saveLetter(FutureLetter letter) {
        if (letter.getUserId() == null) {
            letter.setUserId(1L);
        }
        letter.setStatus("locked");
        letter.setCreatedAt(LocalDateTime.now());
        save(letter);
        return letter;
    }

    @Override
    public boolean removeLetter(Long id, Long userId) {
        return remove(
                new LambdaQueryWrapper<FutureLetter>()
                        .eq(FutureLetter::getId, id)
                        .eq(FutureLetter::getUserId, userId)
        );
    }
}
