package com.example.train.member.service;

import cn.hutool.core.collection.CollUtil;
import com.example.train.common.exception.BusinessException;
import com.example.train.common.exception.BusinessExceptionEnum;
import com.example.train.common.util.SnowUtil;
import com.example.train.member.domain.Member;
import com.example.train.member.domain.MemberExample;
import com.example.train.member.mapper.MemberMapper;
import com.example.train.member.req.MemberRegisterReq;
import com.example.train.member.req.MemberSendCodeReq;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    private static final Logger Log = LoggerFactory.getLogger(MemberService.class);

    @Resource
    private MemberMapper memberMapper;

    public Long count(){
        return memberMapper.countByExample(null);
    }

    public Long register(MemberRegisterReq req)  {
        String mobile=req.getMobile();
        MemberExample memberExample=new MemberExample();
        memberExample.createCriteria().andMobileEqualTo(mobile);
        List<Member> memberList=memberMapper.selectByExample(memberExample);
        if(CollUtil.isNotEmpty(memberList)){
            //return memberList.get(0).getId();
            throw new BusinessException(BusinessExceptionEnum.BUSINESS_MOBILE_EXIST);
        }

        Member member=new Member();
        member.setId(SnowUtil.getSnowflakeNextId());
        member.setMobile(mobile);

        memberMapper.insert(member);
        return member.getId();
    }

    public void sendCode(MemberSendCodeReq req)  {
        String mobile=req.getMobile();
        MemberExample memberExample=new MemberExample();
        memberExample.createCriteria().andMobileEqualTo(mobile);
        List<Member> memberList=memberMapper.selectByExample(memberExample);
        //如果手机号不存在则插入数据
        if(CollUtil.isEmpty(memberList)){
            Log.info("手机号不存在，插入一条记录");
            Member member=new Member();
            member.setId(SnowUtil.getSnowflakeNextId());
            member.setMobile(mobile);
            memberMapper.insert(member);
        }else{
            Log.info("手机号存在，不插入记录");
        }
        //生成验证码
//        String code=RandomUtil.randomString(4);
        String code="8888";
        //保存短信记录表 手机号，短信验证码，有效期，是否已使用，业务类型，发送时间，使用时间

        //对接短信通道、发送短信

    }
}
