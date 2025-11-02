package com.github.jwxa.controller;

import com.github.jwxa.service.DemoService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RMapCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DemoController {

    private final DemoService demoService;

    @Autowired
    private RBucket<Object> demoBucket;


    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/get")
    public String get(@RequestParam String key) {
        return demoService.getData(key);
    }


    @GetMapping("/cscGet")
    public String cscGet() {
        String s = (String) demoBucket.get();
        log.info("[RBucket] GET value={}", s);
        return s;
    }

    @PostMapping("/cscSet")
    public String cscSet(@RequestParam("value")String value) {
        demoBucket.set(value);
        log.info("[RBucket] SET value={}", value);
        return "success";
    }

}