package com.example.train.member.controller;

import com.example.train.member.service.MemberService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
public class MemberController {

    @Resource
    private MemberService memberService;

    @GetMapping("/count")
    public Long count(){return memberService.count();}

    @PostMapping("/register")
    public Long register(String mobile){return memberService.register(mobile);}
}
