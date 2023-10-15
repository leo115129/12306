package com.example.train.member.service;

import cn.hutool.core.collection.CollUtil;
import com.example.train.common.exception.BusinessException;
import com.example.train.common.exception.BusinessExceptionEnum;
import com.example.train.member.domain.Member;
import com.example.train.member.domain.MemberExample;
import com.example.train.member.mapper.MemberMapper;
import com.example.train.member.req.MemberRegisterReq;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

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
        member.setId(System.currentTimeMillis());
        member.setMobile(mobile);

        memberMapper.insert(member);
        return member.getId();
    }
}
