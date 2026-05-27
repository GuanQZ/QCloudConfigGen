package com.configgen.controller;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ShutdownController {

    private static ApplicationContext context;

    public ShutdownController(ApplicationContext ctx) {
        context = ctx;
    }

    @PostMapping("/shutdown")
    public String shutdown() {
        new Thread(() -> {
            try {
                Thread.sleep(500);
                System.exit(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "shutdown-thread").start();
        return "Shutting down...";
    }
}