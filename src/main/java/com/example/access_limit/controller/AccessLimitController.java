package com.example.access_limit.controller;

import com.example.access_limit.annotation.AccessLimit;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccessLimitController {

    @RequestMapping("/limit")
    @AccessLimit(times = 5)
    public String limit() {
        return "success";
    }
}
