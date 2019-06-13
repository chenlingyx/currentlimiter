package com.example.currentlimiter.controller;

import com.example.currentlimiter.annotation.Limit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

/**
* @ClassName : LimitController
* @Description : //TODO 
* @author : chenling
* @Date : 2019/6/12 19:17
* @since : v1.0.0 
**/
@RestController
public class LimitController {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger();

    @Limit(key = "test", period = 100, count = 10)
    @GetMapping("/test")
    public int testLimiter() {
        // 意味著 100S 内最多允許訪問10次
        return ATOMIC_INTEGER.incrementAndGet();
    }


}
