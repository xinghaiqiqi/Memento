package com.memento.service;

import com.memento.entity.User;

public interface InitService {
    boolean isInitialized();
    User setup(String nickname, String apiKey);
}
