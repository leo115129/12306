package com.example.train.member.controller;

import com.example.train.comon.resp.CommonResp;
import com.example.train.member.req.MemberRegisterReq;
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
    public CommonResp<Long> count() {
        Long count=memberService.count();
        return new CommonResp<>(count);
    }

    @PostMapping("/register")
    public CommonResp<Long> register(MemberRegisterReq req) {
        Long register = memberService.register(req);
        return new CommonResp<>(register);
    }
}
