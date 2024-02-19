package com.example.train.member.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.train.common.exception.BusinessException;
import com.example.train.common.exception.BusinessExceptionEnum;
import com.example.train.common.util.JwtUtil;
import com.example.train.common.util.SnowUtil;
import com.example.train.member.domain.Member;
import com.example.train.member.domain.MemberExample;
import com.example.train.member.mapper.MemberMapper;
import com.example.train.member.req.MemberLoginReq;
import com.example.train.member.req.MemberRegisterReq;
import com.example.train.member.req.MemberSendCodeReq;
import com.example.train.member.resp.MemberLoginResp;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.redisson.api.RBloomFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {

    private static final Logger Log = LoggerFactory.getLogger(MemberService.class);

    @Resource
    private MemberMapper memberMapper;

    @Autowired
    private RBloomFilter<String> rBloomFilter;

    public Long count(){
        return memberMapper.countByExample(null);
    }

    public Long register(MemberRegisterReq req)  {
        String mobile=req.getMobile();
        Member memberDB = selectByMobile(mobile);
        if (ObjectUtil.isNull(memberDB)) {
            //return memberList.get(0).getId();
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_EXIST);
        }

        Member member=new Member();
        member.setId(SnowUtil.getSnowflakeNextId());
        member.setMobile(mobile);

        memberMapper.insert(member);
        return member.getId();
    }

    @PostConstruct
    public void initBloomFilter(){
        List<String> allMoblie=memberMapper.getAllMobile();
        for(var i:allMoblie){
            rBloomFilter.add(i);
        //    System.out.println(i);
        }
    }

    public void sendCode(MemberSendCodeReq req)  {
        String mobile=req.getMobile();
        boolean exist=rBloomFilter.contains(mobile);
      //  Member memberDB = selectByMobile(mobile);
        //如果手机号不存在则插入数据
        if (exist==false/*ObjectUtil.isNull(memberDB)*/) {
            Log.info("手机号不存在，插入一条记录");
            Member member=new Member();
            member.setId(SnowUtil.getSnowflakeNextId());
            member.setMobile(mobile);
           // rBloomFilter.add(mobile);
            memberMapper.insert(member);
        }else{
            Log.info("手机号存在，不插入记录");
        }
        String code="8888";
        //生成验证码
        //String code= RandomUtil.randomString(4);
        //保存短信记录表 手机号，短信验证码，有效期，是否已使用，业务类型，发送时间，使用时间
        //对接短信通道、发送短信
    }

    public MemberLoginResp login(MemberLoginReq req) {
        String mobile = req.getMobile();
        String code = req.getCode();
        Member memberDB = selectByMobile(mobile);

        if (ObjectUtil.isNull(memberDB)) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_NOT_EXIST);
        }

        //校验短信验证码
        if(!"8888".equals(code)){
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_CODE_ERROR);
        }

        MemberLoginResp memberLoginResp= BeanUtil.copyProperties(memberDB,MemberLoginResp.class);
        //Map<String, Object> map = BeanUtil.beanToMap(memberLoginResp);
       // String token=JWTUtil.createToken(map,key.getBytes());
        String token= JwtUtil.createToken(memberLoginResp.getId(),memberLoginResp.getMobile());
        memberLoginResp.setToken(token);
        return memberLoginResp;
    }

    private Member selectByMobile(String mobile) {
        MemberExample memberExample = new MemberExample();
        memberExample.createCriteria().andMobileEqualTo(mobile);
        List<Member> memberList=memberMapper.selectByExample(memberExample);
        if (CollUtil.isEmpty(memberList)) {
            return null;
        } else {
            return memberList.get(0);
        }
    }
}
