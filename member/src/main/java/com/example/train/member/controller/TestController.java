package com.example.train.member.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Value("${test.nacos}")
    private String test;

    @GetMapping("/hello")
    public String hello(){return String.format("hello,%s",test);}
}
