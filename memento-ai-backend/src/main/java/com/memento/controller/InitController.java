package com.memento.controller;

import com.memento.dto.Result;
import com.memento.entity.User;
import com.memento.service.InitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/init")
public class InitController {

    @Autowired
    private InitService initService;

    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> res = new HashMap<>();
        res.put("initialized", initService.isInitialized());
        return Result.success(res);
    }

    @PostMapping("/setup")
    public Result<User> setup(@RequestBody Map<String, String> params) {
        String nickname = params.get("nickname");
        String apiKey = params.get("apiKey");
        User user = initService.setup(nickname, apiKey);
        return Result.success(user);
    }
}
