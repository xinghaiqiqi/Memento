package com.memento.controller;

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
    public Map<String, Object> getStatus() {
        Map<String, Object> res = new HashMap<>();
        res.put("initialized", initService.isInitialized());
        return res;
    }

    @PostMapping("/setup")
    public User setup(@RequestBody Map<String, String> params) {
        String nickname = params.get("nickname");
        String apiKey = params.get("apiKey");
        return initService.setup(nickname, apiKey);
    }
}
