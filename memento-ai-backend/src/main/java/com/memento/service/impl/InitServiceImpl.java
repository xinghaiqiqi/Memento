package com.memento.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memento.entity.AppConfig;
import com.memento.entity.User;
import com.memento.mapper.AppConfigMapper;
import com.memento.mapper.UserMapper;
import com.memento.service.InitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InitServiceImpl implements InitService {

    @Autowired
    private AppConfigMapper appConfigMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean isInitialized() {
        AppConfig config = appConfigMapper.selectOne(
                new LambdaQueryWrapper<AppConfig>().eq(AppConfig::getConfigKey, "is_initialized")
        );
        return config != null && "true".equals(config.getConfigValue());
    }

    @Override
    @Transactional
    public User setup(String nickname, String apiKey) {
        // 1. 创建用户
        User user = new User();
        user.setNickname(nickname == null || nickname.isEmpty() ? "记忆收藏家" : nickname);
        userMapper.insert(user);

        // 2. 更新配置
        updateConfig("is_initialized", "true");
        if (apiKey != null && !apiKey.isEmpty()) {
            updateConfig("api_key", apiKey);
        }

        return user;
    }

    private void updateConfig(String key, String value) {
        AppConfig config = appConfigMapper.selectOne(
                new LambdaQueryWrapper<AppConfig>().eq(AppConfig::getConfigKey, key)
        );
        if (config != null) {
            config.setConfigValue(value);
            appConfigMapper.updateById(config);
        } else {
            config = new AppConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            appConfigMapper.insert(config);
        }
    }
}
